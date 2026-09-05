import React, { useState, useEffect, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Users, Search, X, Filter, Eye, Dna, RefreshCw,
  AlertCircle, ChevronDown
} from 'lucide-react';
import { getFhirPatients, getPatientTwins, savePatientTwin, getPatientName, calculateAge } from '../services/api';
import ActionModal from '../components/ActionModal';

export default function PatientsPage() {
  const [patients, setPatients] = useState([]);
  const [twins, setTwins] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [genderFilter, setGenderFilter] = useState('');
  const [savingTwinId, setSavingTwinId] = useState(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [modalData, setModalData] = useState(null);

  const navigate = useNavigate();

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [patientsData, twinsData] = await Promise.all([
        getFhirPatients(),
        getPatientTwins().catch(() => [])
      ]);
      setPatients(Array.isArray(patientsData) ? patientsData : []);
      setTwins(Array.isArray(twinsData) ? twinsData : []);
    } catch (err) {
      console.error('Failed to load patients:', err);
      setError('Unable to load patients from FHIR server. Please ensure the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const hasTwin = (patientId) => {
    return twins.some(t => String(t.patientId) === String(patientId));
  };

  const handleCreateTwin = async (patient) => {
    setSavingTwinId(patient.id);
    try {
      const createdTwin = await savePatientTwin(patient.id);
      setTwins(prev => {
        const filtered = prev.filter(t => String(t.patientId) !== String(patient.id));
        return [...filtered, createdTwin];
      });
      setModalData({
        title: 'Digital Twin Created',
        message: `Successfully created Digital Health Twin for ${getPatientName(patient)} (ID: ${patient.id}).`,
        type: 'success'
      });
      setModalOpen(true);
    } catch (err) {
      setModalData({
        title: 'Failed to Create Twin',
        message: err.message || 'Failed to save digital twin.',
        type: 'warning'
      });
      setModalOpen(true);
    } finally {
      setSavingTwinId(null);
    }
  };

  const filteredPatients = useMemo(() => {
    return patients.filter(p => {
      const name = getPatientName(p).toLowerCase();
      const id = (p.id || '').toLowerCase();
      const term = searchTerm.toLowerCase();
      const matchesSearch = !term || name.includes(term) || id.includes(term);
      const matchesGender = !genderFilter || (p.gender || '').toLowerCase() === genderFilter.toLowerCase();
      return matchesSearch && matchesGender;
    });
  }, [patients, searchTerm, genderFilter]);

  const clearFilters = () => {
    setSearchTerm('');
    setGenderFilter('');
  };

  const hasActiveFilters = searchTerm || genderFilter;

  return (
    <div className="dashboard-content">
      <div className="page-header-row">
        <div className="page-title-group">
          <h1>Patients</h1>
          <p>FHIR Patient Records from HL7 Server</p>
        </div>
        <button className="btn-secondary" onClick={loadData} disabled={loading}>
          <RefreshCw size={16} className={loading ? 'spin-icon' : ''} />
          Refresh
        </button>
      </div>

      {error && (
        <div className="patients-error-banner">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Search & Filter Bar */}
      <div className="patients-toolbar">
        <div className="patients-search-wrapper">
          <Search size={18} className="patients-search-icon" />
          <input
            type="text"
            placeholder="Search by name or patient ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="patients-search-input"
          />
          {searchTerm && (
            <button className="patients-search-clear" onClick={() => setSearchTerm('')}>
              <X size={16} />
            </button>
          )}
        </div>
        <div className="patients-filter-group">
          <div className="patients-select-wrapper">
            <Filter size={16} />
            <select
              value={genderFilter}
              onChange={(e) => setGenderFilter(e.target.value)}
              className="patients-filter-select"
            >
              <option value="">All Genders</option>
              <option value="male">Male</option>
              <option value="female">Female</option>
              <option value="other">Other</option>
            </select>
            <ChevronDown size={14} className="patients-select-chevron" />
          </div>
          {hasActiveFilters && (
            <button className="patients-clear-btn" onClick={clearFilters}>
              <X size={14} />
              Clear Filters
            </button>
          )}
        </div>
      </div>

      {/* Patients Table */}
      {loading ? (
        <div className="patients-loading">
          <div className="patients-loading-spinner" />
          <p>Loading patients from FHIR server...</p>
        </div>
      ) : filteredPatients.length === 0 ? (
        <div className="patients-empty">
          <Users size={48} />
          <h3>{patients.length === 0 ? 'No Patients Found' : 'No Matching Patients'}</h3>
          <p>
            {patients.length === 0
              ? 'No patient records available from the FHIR server.'
              : 'Try adjusting your search or filter criteria.'}
          </p>
          {hasActiveFilters && (
            <button className="btn-secondary" onClick={clearFilters} style={{ marginTop: '12px' }}>
              Clear Filters
            </button>
          )}
        </div>
      ) : (
        <div className="patients-table-wrapper">
          <table className="patients-table">
            <thead>
              <tr>
                <th>Patient ID</th>
                <th>Name</th>
                <th>Age</th>
                <th>Gender</th>
                <th>Date of Birth</th>
                <th>Twin Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredPatients.map((patient) => {
                const name = getPatientName(patient);
                const age = calculateAge(patient.birthDate);
                const gender = patient.gender
                  ? patient.gender.charAt(0).toUpperCase() + patient.gender.slice(1)
                  : 'N/A';
                const twinExists = hasTwin(patient.id);
                const isSaving = savingTwinId === patient.id;

                return (
                  <tr key={patient.id}>
                    <td className="patients-td-id">
                      <code>{patient.id}</code>
                    </td>
                    <td className="patients-td-name">{name}</td>
                    <td>{age !== 'N/A' ? `${age} yrs` : 'N/A'}</td>
                    <td>
                      <span className={`patients-gender-badge ${(patient.gender || '').toLowerCase()}`}>
                        {gender}
                      </span>
                    </td>
                    <td>{patient.birthDate || 'N/A'}</td>
                    <td>
                      {twinExists ? (
                        <span className="patients-twin-badge active">Twin Active</span>
                      ) : (
                        <span className="patients-twin-badge inactive">No Twin</span>
                      )}
                    </td>
                    <td className="patients-td-actions">
                      <button
                        className="patients-action-btn view"
                        onClick={() => navigate(`/patients/${patient.id}`)}
                        title="View Details"
                      >
                        <Eye size={15} />
                        View
                      </button>
                      <button
                        className="patients-action-btn twin"
                        onClick={() => handleCreateTwin(patient)}
                        disabled={isSaving}
                        title={twinExists ? 'Update Twin' : 'Create Twin'}
                      >
                        <Dna size={15} />
                        {isSaving ? 'Saving...' : twinExists ? 'Update' : 'Create Twin'}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          <div className="patients-table-footer">
            Showing {filteredPatients.length} of {patients.length} patients
          </div>
        </div>
      )}

      <ActionModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        modalData={modalData}
      />
    </div>
  );
}
