import type { Page, Theme } from '../types/banking';
import { getInitials } from '../utils/formatters';

const titles: Record<Page, string> = {
  dashboard: 'Главная', accounts: 'Счета', cards: 'Карты', payments: 'Платежи и переводы', currency: 'Валюта', news: 'Финансовые новости', assistant: 'Ассистент', profile: 'Профиль',
};

interface HeaderProps { activePage: Page; theme: Theme; userName: string; onThemeToggle: () => void; onReset: () => void; onLogout: () => void; }

function Header({ activePage, theme, userName, onThemeToggle, onReset, onLogout }: HeaderProps) {
  return (
    <header className="header">
      <div><p className="header__eyebrow">МИК Банк</p><h1>{titles[activePage]}</h1></div>
      <div className="header__right">
        <div className="header__search">⌘K Поиск по операциям</div>
        <button className="icon-button" type="button" onClick={onThemeToggle} title="Сменить тему">{theme === 'dark' ? '☀' : '☾'}</button>
        <button className="icon-button icon-button--wide" type="button" onClick={onReset}>Сброс</button>
        <button className="icon-button icon-button--wide" type="button" onClick={onLogout}>Выйти</button>
        <div className="header__user"><span className="header__avatar">{getInitials(userName)}</span><span>{userName.split(' ')[0]}</span></div>
      </div>
    </header>
  );
}

export default Header;
