import React, { useState, useEffect } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './services/AuthContext';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import Patient360Dashboard from './pages/Patient360Dashboard';
import PatientsPage from './pages/PatientsPage';
import PatientDetailPage from './pages/PatientDetailPage';
import DigitalTwinsPage from './pages/DigitalTwinsPage';
import RiskPredictionsPage from './pages/RiskPredictionsPage';
import ShapExplainabilityPage from './pages/ShapExplainabilityPage';
import AnalyticsPage from './pages/AnalyticsPage';
import ModelInfoPage from './pages/ModelInfoPage';
import PlaceholderPage from './pages/PlaceholderPage';
import MonitoringDashboard from './pages/MonitoringDashboard';

import {
  TrendingUp,
  Bell,
  ClipboardList,
  FileBarChart
} from 'lucide-react';

function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, hasRole } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !hasRole(allowedRoles)) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

function AuthenticatedLayout() {
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('medisphere_theme') || 'light';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('medisphere_theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => (prev === 'light' ? 'dark' : 'light'));
  };

  return (
    <div className="app-container">
      <Header theme={theme} onToggleTheme={toggleTheme} />
      <div className="main-wrapper">
        <Sidebar />
        <main style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'auto' }}>
          <Routes>
            <Route path="/dashboard" element={
              <ProtectedRoute>
                <Patient360Dashboard />
              </ProtectedRoute>
            } />
            <Route path="/patients" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <PatientsPage />
              </ProtectedRoute>
            } />
            <Route path="/patients/:id" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <PatientDetailPage />
              </ProtectedRoute>
            } />
            <Route path="/twins" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <DigitalTwinsPage />
              </ProtectedRoute>
            } />
            <Route path="/risk-predictions" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <RiskPredictionsPage />
              </ProtectedRoute>
            } />
            <Route path="/shap" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <ShapExplainabilityPage />
              </ProtectedRoute>
            } />
            <Route path="/analytics" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <AnalyticsPage />
              </ProtectedRoute>
            } />
            <Route path="/alerts" element={
              <ProtectedRoute>
                <MonitoringDashboard />
              </ProtectedRoute>
            } />
            <Route path="/care-plans" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <PlaceholderPage
                  title="Care Plans"
                  description="Patient care plan management and tracking"
                  icon={ClipboardList}
                />
              </ProtectedRoute>
            } />
            <Route path="/reports" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <PlaceholderPage
                  title="Reports"
                  description="Clinical reports and documentation"
                  icon={FileBarChart}
                />
              </ProtectedRoute>
            } />
            <Route path="/model-info" element={
              <ProtectedRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <ModelInfoPage />
              </ProtectedRoute>
            } />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default function App() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  // Public routes
  const publicPaths = ['/', '/login', '/signup'];
  const isPublicPath = publicPaths.includes(location.pathname);

  // If user is on a public route
  if (isPublicPath) {
    return (
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={
          isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />
        } />
        <Route path="/signup" element={
          isAuthenticated ? <Navigate to="/dashboard" replace /> : <SignupPage />
        } />
      </Routes>
    );
  }

  // Protected routes — require auth
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <AuthenticatedLayout />;
}
