// API Client for MediSphere Backend & FHIR Integration

const USER_PROFILE_KEY = 'medisphere_user_profile';
const AUTH_KEY = 'medisphere_session_auth';

let sessionCredentials = (() => {
  try {
    const item = localStorage.getItem(AUTH_KEY);
    return item ? JSON.parse(item) : null;
  } catch {
    return null;
  }
})();

export function getStoredAuth() {
  if (!sessionCredentials) {
    try {
      const item = localStorage.getItem(AUTH_KEY);
      sessionCredentials = item ? JSON.parse(item) : null;
    } catch {
      sessionCredentials = null;
    }
  }
  return sessionCredentials;
}

export function setStoredAuth(credentials) {
  sessionCredentials = credentials || null;
  if (credentials) {
    try { localStorage.setItem(AUTH_KEY, JSON.stringify(credentials)); } catch {}
  } else {
    try { localStorage.removeItem(AUTH_KEY); } catch {}
  }
}

export function getStoredProfile() {
  const stored = localStorage.getItem(USER_PROFILE_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {
      return null;
    }
  }
  return null;
}

export function setStoredProfile(profile) {
  if (!profile) {
    localStorage.removeItem(USER_PROFILE_KEY);
  } else {
    localStorage.setItem(USER_PROFILE_KEY, JSON.stringify(profile));
  }
}

/**
 * Get the currently authenticated user info (id, fullName, username, role, createdAt)
 */
export function getCurrentUser() {
  const profile = getStoredProfile();
  const auth = getStoredAuth();
  if (profile && profile.username && auth && auth.username) {
    return profile;
  }
  return null;
}

/**
 * Clear all authentication data
 */
export function clearAuth() {
  localStorage.removeItem(USER_PROFILE_KEY);
  try { localStorage.removeItem(AUTH_KEY); } catch {}
  sessionCredentials = null;
}

function getAuthHeader() {
  const auth = getStoredAuth();
  if (!auth || !auth.username) return {};
  const encoded = btoa(`${auth.username}:${auth.password}`);
  return { 'Authorization': `Basic ${encoded}` };
}

async function request(url, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...getAuthHeader(),
    ...(options.headers || {})
  };

  try {
    const response = await fetch(url, { ...options, headers });

    if (!response.ok) {
      let errorMessage = '';
      try {
        const text = await response.text();
        try {
          const errorJson = JSON.parse(text);
          errorMessage = errorJson.message || errorJson.error || text;
        } catch {
          errorMessage = text;
        }
      } catch {
        // ignore read error
      }

      if (response.status === 401) {
        throw new Error('Your session has expired. Please log in again.');
      }

      if (response.status === 403) {
        if (errorMessage && errorMessage.toLowerCase().includes('consent')) {
          throw new Error("Patient consent is required to access this patient's data.");
        }
        throw new Error(errorMessage || 'Access denied.');
      }

      if (response.status === 503) {
        throw new Error('AI prediction service is currently unavailable.');
      }

      if (response.status >= 500) {
        if (errorMessage && (errorMessage.toLowerCase().includes('connect') || errorMessage.toLowerCase().includes('service unavailable') || errorMessage.toLowerCase().includes('flask'))) {
          throw new Error('AI prediction service is currently unavailable.');
        }
        throw new Error(errorMessage || 'Unable to generate prediction. Please try again.');
      }

      throw new Error(errorMessage || `HTTP Error ${response.status}`);
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    }
    return await response.text();
  } catch (err) {
    if (err.name === 'TypeError' && err.message.toLowerCase().includes('fetch')) {
      throw new Error('AI prediction service is currently unavailable.');
    }
    console.error(`API Error on ${url}:`, err);
    throw err;
  }
}

/**
 * Register a new user via POST /api/auth/signup
 */
export async function signupUser(fullName, username, password, role = 'DOCTOR') {
  const response = await fetch('/api/auth/signup', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      fullName: fullName.trim(),
      username: username.trim(),
      password,
      role
    })
  });

  if (!response.ok) {
    const text = await response.text();
    let errorMessage = text;
    try {
      const errorJson = JSON.parse(text);
      errorMessage = errorJson.message || errorJson.error || text;
    } catch {
      // use raw text
    }
    throw new Error(errorMessage || 'Signup failed. Please try again.');
  }

  return await response.json();
}

/**
 * Authenticate user via POST /api/auth/login
 * Saves credentials for Basic Auth and stores user profile.
 */
export async function loginUser(username, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      username: username.trim(),
      password
    })
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Invalid username or password.');
    }
    const text = await response.text();
    let errorMessage = text;
    try {
      const errorJson = JSON.parse(text);
      errorMessage = errorJson.message || errorJson.error || text;
    } catch {
      // use raw text
    }
    throw new Error(errorMessage || 'Login failed.');
  }

  const userProfile = await response.json();

  // Store credentials for downstream basic-auth requests & store user profile
  setStoredAuth({ username: username.trim(), password });
  setStoredProfile(userProfile);

  return userProfile;
}

/**
 * Fetch all patients from FHIR server via backend controller
 */
export async function getFhirPatients() {
  return await request('/api/fhir/patients');
}

/**
 * Fetch all ML demo patients (Milestone 2 synthetic patients for risk prediction)
 * Returns ML001-ML005 with pre-computed feature values for CVD and Diabetes models
 */
export async function getMLDemoPatients() {
  return await request('/api/ml/demo-patients');
}

/**
 * Fetch a specific ML demo patient by ID
 */
export async function getMLDemoPatient(patientId) {
  return await request(`/api/ml/demo-patients/${encodeURIComponent(patientId)}`);
}

/**
 * Save/Create PatientTwin from FHIR Patient ID
 */
export async function savePatientTwin(patientId) {
  return await request(`/api/fhir/patients/${encodeURIComponent(patientId)}/save-twin`);
}

/**
 * Fetch all saved Patient Twins from backend
 */
export async function getPatientTwins() {
  return await request('/api/patient-twins');
}

/**
 * Fetch a specific Patient Twin by ID
 */
export async function getPatientTwinById(id) {
  return await request(`/api/patient-twins/${encodeURIComponent(id)}`);
}

export async function getPatient360(sourcePatientId) {
  return await request(`/api/patient-360/${encodeURIComponent(sourcePatientId)}`);
}

/**
 * Helper to extract display name from FHIR Patient resource or DTO
 */
export function getPatientName(patient) {
  if (!patient) return 'Unknown Patient';
  // Handle simplified DTO where name is a plain string
  if (typeof patient.name === 'string') {
    return patient.name || `Patient #${patient.id || 'N/A'}`;
  }
  // Handle original FHIR HumanName array format
  if (patient.name && patient.name.length > 0) {
    const nameObj = patient.name[0];
    if (nameObj.text) return nameObj.text;
    const given = nameObj.given ? nameObj.given.join(' ') : '';
    const family = nameObj.family || '';
    const full = `${given} ${family}`.trim();
    if (full) return full;
  }
  return `Patient #${patient.id || 'N/A'}`;
}

/**
 * Helper to calculate age from birthDate
 */
export function calculateAge(birthDateStr) {
  if (!birthDateStr) return 'N/A';
  const birthDate = new Date(birthDateStr);
  if (isNaN(birthDate.getTime())) return 'N/A';
  const ageDifMs = Date.now() - birthDate.getTime();
  const ageDate = new Date(ageDifMs);
  return Math.abs(ageDate.getUTCFullYear() - 1970);
}

// ============================================================
// ML Prediction API
// ============================================================

/**
 * Run CVD risk prediction via Spring Boot ML proxy.
 * POST /api/ml/predict/cvd — expects { features: number[] } (15 features)
 */
export async function predictCvd(features) {
  return await request('/api/ml/predict/cvd', {
    method: 'POST',
    body: JSON.stringify({ features }),
  });
}

/**
 * Run Diabetes risk prediction via Spring Boot ML proxy.
 * POST /api/ml/predict/diabetes — expects { features: number[] } (8 features)
 */
export async function predictDiabetes(features) {
  return await request('/api/ml/predict/diabetes', {
    method: 'POST',
    body: JSON.stringify({ features }),
  });
}

/**
 * Fetch model metadata from Spring Boot ML proxy.
 * GET /api/ml/models/:modelName — modelName is "cvd" or "diabetes"
 */
export async function getModelMetadata(modelName) {
  return await request(`/api/ml/models/${encodeURIComponent(modelName)}`);
}

// ============================================================
// Milestone 3 — Monitoring Dashboard API
// ============================================================

/**
 * Fetch all active alerts (status = ACTIVE), newest first.
 * GET /api/alerts/active
 */
export async function getActiveAlerts() {
  return await request('/api/alerts/active');
}

/**
 * Fetch alerts for a specific patient.
 * GET /api/alerts/patient/{patientId}
 */
export async function getPatientAlerts(patientId) {
  return await request(`/api/alerts/patient/${encodeURIComponent(patientId)}`);
}

/**
 * Acknowledge an alert by ID.
 * PUT /api/alerts/{id}/acknowledge
 */
export async function acknowledgeAlert(alertId) {
  return await request(`/api/alerts/${encodeURIComponent(alertId)}/acknowledge`, {
    method: 'PUT',
  });
}

/**
 * Fetch the latest vital records for a patient (up to 50).
 * GET /api/vital-records/{patientId}/latest
 */
export async function getLatestVitals(patientId) {
  return await request(`/api/vital-records/${encodeURIComponent(patientId)}/latest`);
}

