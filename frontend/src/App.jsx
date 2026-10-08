import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import { useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import CitizenDashboard from './pages/CitizenDashboard';
import ReportProblem from './pages/ReportProblem';
import TrackReports from './pages/TrackReports';
import OfficerQueue from './pages/OfficerQueue';
import AdminAnalytics from './pages/AdminAnalytics';

function RoleHome() {
  const { user } = useAuth();
  if (user?.role === 'ADMIN') return <Navigate to="/admin" replace />;
  if (user?.role === 'OFFICER') return <Navigate to="/officer" replace />;
  return <Layout><CitizenDashboard /></Layout>;
}

function GuestRoute({ children }) {
  const { user } = useAuth();
  if (!user) return children;
  const homePath = user.role === 'ADMIN' ? '/admin' : user.role === 'OFFICER' ? '/officer' : '/';
  return <Navigate to={homePath} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<GuestRoute><Login /></GuestRoute>} />
      <Route path="/register" element={<GuestRoute><Register /></GuestRoute>} />

      <Route path="/" element={
        <ProtectedRoute><RoleHome /></ProtectedRoute>
      } />
      <Route path="/report" element={
        <ProtectedRoute roles={['CITIZEN']}><Layout><ReportProblem /></Layout></ProtectedRoute>
      } />
      <Route path="/track" element={
        <ProtectedRoute roles={['CITIZEN']}><Layout><TrackReports /></Layout></ProtectedRoute>
      } />
      <Route path="/officer" element={
        <ProtectedRoute roles={['OFFICER', 'ADMIN']}><Layout><OfficerQueue /></Layout></ProtectedRoute>
      } />
      <Route path="/admin" element={
        <ProtectedRoute roles={['ADMIN']}><Layout><AdminAnalytics /></Layout></ProtectedRoute>
      } />
    </Routes>
  );
}
