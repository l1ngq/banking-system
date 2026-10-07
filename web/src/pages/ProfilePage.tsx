import { useState } from 'react';

import { UiIcon } from '../components/operations/Icon';
import type { Theme, UserProfile } from '../types/banking';
import { getInitials, pluralize } from '../utils/formatters';

interface ProfilePageProps {
  profile: UserProfile;
  accountsCount: number;
  onUpdate: (payload: Pick<UserProfile, 'fullName' | 'phone' | 'city'>) => Promise<boolean>;
  onThemeChange: (theme: Theme) => void;
  onToggleBalance: () => void;
  onLogout: () => void;
}

function ProfileForm({ profile, onUpdate }: Pick<ProfilePageProps, 'profile' | 'onUpdate'>) {
  const [name, setName] = useState(profile.fullName);
  const [phone, setPhone] = useState(profile.phone);
  const [city, setCity] = useState(profile.city);
  const [saving, setSaving] = useState(false);

  const phoneDigits = phone.replace(/\D/g, '');
  const phoneError = phone && (phoneDigits.length < 10 || phoneDigits.length > 12) ? 'Введите номер полностью, например +7 900 123-45-67' : '';
  const nameError = !name.trim() ? 'Имя не может быть пустым' : '';
  const dirty = name !== profile.fullName || phone !== profile.phone || city !== profile.city;

  return (
    <div className="form">
      <label className="field">
        <span className="field__label">Как к вам обращаться</span>
        <input className={`input${nameError ? ' is-invalid' : ''}`} value={name} maxLength={60} onChange={(event) => setName(event.target.value)} />
        {nameError && <p className="error-text">{nameError}</p>}
      </label>
      <label className="field">
        <span className="field__label">Телефон</span>
        <input className={`input${phoneError ? ' is-invalid' : ''}`} inputMode="tel" value={phone} placeholder="+7 900 123-45-67" onChange={(event) => setPhone(event.target.value)} />
        {phoneError && <p className="error-text">{phoneError}</p>}
      </label>
      <label className="field">
        <span className="field__label">Город</span>
        <input className="input" value={city} maxLength={60} placeholder="Например, Москва" onChange={(event) => setCity(event.target.value)} />
      </label>
      <div>
        <button
          type="button"
          className="btn btn--primary"
          disabled={!dirty || saving || Boolean(phoneError) || Boolean(nameError)}
          onClick={() => {
            setSaving(true);
            void onUpdate({ fullName: name.trim(), phone: phone.trim(), city: city.trim() }).finally(() => setSaving(false));
          }}
        >
          {saving ? 'Сохраняем…' : 'Сохранить изменения'}
        </button>
      </div>
    </div>
  );
}

function ProfilePage({ profile, accountsCount, onUpdate, onThemeChange, onToggleBalance, onLogout }: ProfilePageProps) {
  const formKey = `${profile.email}-${profile.fullName}-${profile.phone}-${profile.city}`;

  return (
    <div className="page">
      <section className="card">
        <div className="profile-head">
          <span className="avatar avatar--lg">{getInitials(profile.fullName)}</span>
          <div>
            <h2>{profile.fullName || 'Пользователь'}</h2>
            <span className="muted">{profile.email}</span>
            <div className="chips" style={{ marginTop: 8 }}>
              <span className="badge badge--brand">{profile.role === 'admin' ? 'Администратор' : 'Клиент банка'}</span>
              <span className="badge">
                {accountsCount} {pluralize(accountsCount, 'активный счёт', 'активных счёта', 'активных счетов')}
              </span>
            </div>
          </div>
        </div>
      </section>

      <div className="dash-grid">
        <section className="card">
          <div className="card__head">
            <h2>Личные данные</h2>
          </div>
          <ProfileForm key={formKey} profile={profile} onUpdate={onUpdate} />
        </section>

        <div className="page">
          <section className="card">
            <div className="card__head">
              <h2>Настройки</h2>
            </div>
            <div className="settings">
              <div className="setting">
                <span className="setting__text">
                  <strong>Тема оформления</strong>
                  <span>Тёмная удобнее вечером</span>
                </span>
                <div className="segmented" role="radiogroup" aria-label="Тема">
                  <button type="button" role="radio" aria-checked={profile.theme === 'light'} className={profile.theme === 'light' ? 'is-active' : undefined} onClick={() => onThemeChange('light')}>
                    Светлая
                  </button>
                  <button type="button" role="radio" aria-checked={profile.theme === 'dark'} className={profile.theme === 'dark' ? 'is-active' : undefined} onClick={() => onThemeChange('dark')}>
                    Тёмная
                  </button>
                </div>
              </div>
              <div className="setting">
                <span className="setting__text">
                  <strong>Скрывать баланс на главной</strong>
                  <span>Суммы откроются по нажатию на значок глаза</span>
                </span>
                <button
                  type="button"
                  role="switch"
                  aria-checked={Boolean(profile.hideBalance)}
                  aria-label="Скрывать баланс на главной"
                  className={`switch${profile.hideBalance ? ' is-on' : ''}`}
                  onClick={onToggleBalance}
                />
              </div>
            </div>
          </section>

          <section className="card">
            <div className="card__head">
              <h2>Безопасность</h2>
            </div>
            <dl className="facts">
              <div>
                <dt>Вход</dt>
                <dd>По почте и паролю</dd>
              </div>
              <div>
                <dt>Сессия</dt>
                <dd>Хранится на сервере</dd>
              </div>
              <div>
                <dt>Защита запросов</dt>
                <dd>CSRF-токен</dd>
              </div>
            </dl>
            <button type="button" className="btn btn--danger" style={{ marginTop: 16 }} onClick={onLogout}>
              <UiIcon name="logout" size={18} />
              Выйти из аккаунта
            </button>
          </section>
        </div>
      </div>
    </div>
  );
}

export default ProfilePage;
