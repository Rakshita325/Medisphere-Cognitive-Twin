import React from 'react';
import { Construction } from 'lucide-react';

/**
 * Reusable placeholder page for features that are not yet implemented.
 * Displays a clean "Coming Soon" state with the provided title and description.
 */
export default function PlaceholderPage({ title, description, icon: Icon }) {
  const DisplayIcon = Icon || Construction;

  return (
    <div className="dashboard-content">
      <div className="page-header-row">
        <div className="page-title-group">
          <h1>{title || 'Coming Soon'}</h1>
          <p>{description || 'This feature is under development.'}</p>
        </div>
      </div>

      <div className="placeholder-page-body">
        <div className="placeholder-icon-wrapper">
          <DisplayIcon size={48} />
        </div>
        <h2>Coming Soon</h2>
        <p>
          This section is planned for a future release of MediSphere.
        </p>
        <p className="placeholder-subtext">
          Check back later for updates, or contact your administrator for more information.
        </p>
      </div>
    </div>
  );
}
