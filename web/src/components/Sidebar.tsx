import { navItems } from '../data/mockData';
import type { Page } from '../types/banking';

interface SidebarProps {
  activePage: Page;
  onNavigate: (page: Page) => void;
}

function Sidebar({ activePage, onNavigate }: SidebarProps) {
  return (
    <aside className="sidebar">
      <button className="sidebar__brand" type="button" onClick={() => onNavigate('dashboard')}>
        <span className="sidebar__logo-mark">М</span>
        <span>
          <span className="sidebar__logo-text">МИК Банк</span>
          <span className="sidebar__logo-subtitle">личный кабинет</span>
        </span>
      </button>

      <nav className="sidebar__nav">
        {navItems.map((item) => (
          <button
            key={item.key}
            className={`sidebar__nav-item ${activePage === item.key ? 'sidebar__nav-item--active' : ''}`}
            type="button"
            onClick={() => onNavigate(item.key)}
          >
            <span className="sidebar__nav-icon">{item.icon}</span>
            <span>{item.label}</span>
          </button>
        ))}
      </nav>

      <button className="sidebar__promo" type="button" onClick={() => onNavigate('dashboard')}>
        <span className="sidebar__promo-label">Личный кабинет</span>
        <span className="sidebar__promo-title">МИК Банк</span>
        <span className="sidebar__promo-text">Все основные операции собраны в одном месте.</span>
      </button>
    </aside>
  );
}

export default Sidebar;