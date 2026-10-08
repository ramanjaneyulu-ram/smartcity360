package com.smartcity360.controller;

import com.smartcity360.dto.*;
import com.smartcity360.security.UserDetailsImpl;
import com.smartcity360.service.ClassificationService;
import com.smartcity360.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final ClassificationService classificationService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /** Live preview used by the "Report a Problem" screen as the citizen types. */
    @PostMapping("/classify")
    public ClassifyResponse classify(@Valid @RequestBody ClassifyRequest req) {
        return classificationService.classify(req.getDescription());
    }

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CITIZEN')")
    public Map<String, String> uploadPhoto(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 10L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be between 1 byte and 10 MB");
        }

        String extension = detectImageExtension(file);

        String fileName = UUID.randomUUID() + extension;
        Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        file.transferTo(directory.resolve(fileName));
        return Map.of("photoUrl", "/complaints/photos/" + fileName);
    }

    @GetMapping("/photos/{fileName}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String fileName,
                                              @AuthenticationPrincipal UserDetailsImpl principal) {
        if (!fileName.matches("[0-9a-fA-F-]+\\.(jpg|png|gif)")) {
            return ResponseEntity.notFound().build();
        }

        Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path path = directory.resolve(fileName).normalize();
        if (!path.startsWith(directory) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        complaintService.authorizePhotoAccess("/complaints/photos/" + fileName, principal.getUser());

        MediaType mediaType = switch (fileName.substring(fileName.lastIndexOf('.') + 1)) {
            case "jpg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.IMAGE_GIF;
        };
        return ResponseEntity.ok().contentType(mediaType).body(new FileSystemResource(path));
    }

    private String detectImageExtension(MultipartFile file) {
        try (InputStream input = file.getInputStream(); ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not a supported image");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not a supported image");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > 40_000_000L) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image dimensions are too large");
                }
                return switch (format) {
                    case "jpeg", "jpg" -> ".jpg";
                    case "png" -> ".png";
                    case "gif" -> ".gif";
                    default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image type");
                };
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not a valid image", ex);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('CITIZEN')")
    public ComplaintResponse create(@Valid @RequestBody ComplaintCreateRequest req,
                                     @AuthenticationPrincipal UserDetailsImpl principal) {
        return complaintService.createComplaint(principal.getUser(), req);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CITIZEN')")
    public List<ComplaintResponse> myComplaints(@AuthenticationPrincipal UserDetailsImpl principal) {
        return complaintService.getMyComplaints(principal.getUser());
    }

    @GetMapping("/{id}")
    public ComplaintResponse getById(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetailsImpl principal) {
        return complaintService.getById(id, principal.getUser());
    }

    @GetMapping("/{id}/history")
    public List<ComplaintHistoryResponse> history(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetailsImpl principal) {
        return complaintService.getHistory(id, principal.getUser());
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasRole('CITIZEN')")
    public ComplaintResponse feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest req,
                                       @AuthenticationPrincipal UserDetailsImpl principal) {
        return complaintService.submitFeedback(id, req, principal.getUser());
    }
}
