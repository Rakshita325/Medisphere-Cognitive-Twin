import React from 'react';
import { Link } from 'react-router-dom';
import {
  Activity, Shield, Database, Radio, Heart,
  Lock, Server, Users, ArrowRight, LogIn
} from 'lucide-react';

const features = [
  {
    icon: Server,
    title: 'FHIR Integration',
    description: 'HL7 FHIR R4 compliant data exchange with real healthcare interoperability standards.',
    color: '#3b82f6'
  },
  {
    icon: Users,
    title: 'Digital Health Twin',
    description: 'Create and manage patient digital twins persisted in MongoDB for clinical analysis.',
    color: '#8b5cf6'
  },
  {
    icon: Heart,
    title: 'Real-Time Vitals',
    description: 'Stream and monitor patient vital signs in real time from connected medical devices.',
    color: '#ef4444'
  },
  {
    icon: Radio,
    title: 'Kafka Streaming',
    description: 'Apache Kafka-powered event streaming for high-throughput clinical data pipelines.',
    color: '#f59e0b'
  },
  {
    icon: Database,
    title: 'MongoDB',
    description: 'Secure, scalable document-based storage for patient twins and clinical records.',
    color: '#10b981'
  },
  {
    icon: Shield,
    title: 'Consent Management',
    description: 'Patient consent tracking and governance to ensure ethical data handling.',
    color: '#06b6d4'
  },
  {
    icon: Lock,
    title: 'Secure RBAC',
    description: 'Role-based access control with Spring Security for enterprise-grade authorization.',
    color: '#6366f1'
  }
];

export default function LandingPage() {
  return (
    <div className="landing-page">
      {/* Navigation */}
      <nav className="landing-nav">
        <div className="landing-nav-inner">
          <div className="landing-brand">
            <Activity size={28} className="landing-brand-icon" />
            <span>MediSphere Cognitive Twin</span>
          </div>
          <div className="landing-nav-links">
            <a href="#features" className="landing-nav-link">Home</a>
            <Link to="/login" className="landing-nav-link">Login</Link>
            <Link to="/signup" className="landing-nav-link landing-nav-cta">Sign Up</Link>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="landing-hero">
        <div className="landing-hero-inner">
          <div className="landing-hero-badge">
            <Activity size={16} />
            <span>AI-Powered Digital Healthcare Twin Platform</span>
          </div>
          <h1 className="landing-hero-title">
            MediSphere<br />
            <span className="landing-hero-highlight">Cognitive Twin</span>
          </h1>
          <p className="landing-hero-subtitle">
            Transforming healthcare data into a secure, connected Digital Health Twin.
          </p>
          <div className="landing-hero-actions">
            <Link to="/signup" className="btn-primary landing-hero-btn">
              Get Started
              <ArrowRight size={18} />
            </Link>
            <Link to="/login" className="btn-secondary landing-hero-btn">
              <LogIn size={18} />
              Login
            </Link>
          </div>
        </div>
        <div className="landing-hero-glow" />
      </section>

      {/* Features */}
      <section className="landing-features" id="features">
        <div className="landing-features-inner">
          <div className="landing-section-header">
            <h2>Platform Capabilities</h2>
            <p>Built on modern healthcare interoperability standards and enterprise-grade infrastructure.</p>
          </div>
          <div className="landing-features-grid">
            {features.map((feature) => {
              const Icon = feature.icon;
              return (
                <div key={feature.title} className="landing-feature-card">
                  <div
                    className="landing-feature-icon"
                    style={{ backgroundColor: `${feature.color}15`, color: feature.color }}
                  >
                    <Icon size={24} />
                  </div>
                  <h3>{feature.title}</h3>
                  <p>{feature.description}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="landing-footer">
        <div className="landing-footer-inner">
          <div className="landing-footer-brand">
            <Activity size={20} />
            <span>MediSphere Cognitive Twin</span>
          </div>
          <p>AI-Powered Digital Healthcare Twin Platform</p>
        </div>
      </footer>
    </div>
  );
}
