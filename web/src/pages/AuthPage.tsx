import { FormEvent, useMemo, useState } from 'react';
import { DEMO_USERS, type DemoUser } from '../api/config';

type AuthMode = 'login' | 'register';

interface AuthPageProps {
  loading?: boolean;
  error?: string | null;
  onLogin: (email: string, password: string) => Promise<void>;
  onRegister: (email: string, password: string) => Promise<void>;
  onDemoLogin: (user: DemoUser) => Promise<void>;
}

function AuthPage({ loading = false, error, onLogin, onRegister, onDemoLogin }: AuthPageProps) {
  const [mode, setMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState(DEMO_USERS[0]?.email ?? 'igor@example.com');
  const [password, setPassword] = useState(DEMO_USERS[0]?.password ?? 'password123');
  const [localError, setLocalError] = useState<string | null>(null);

  const isRegister = mode === 'register';

  const title = useMemo(
    () => (isRegister ? 'Создайте аккаунт' : 'Войдите в МИК Банк'),
    [isRegister],
  );

  const subtitle = useMemo(
    () =>
      isRegister
        ? 'Укажите почту и пароль, чтобы открыть личный кабинет.'
        : 'Введите данные или выберите демо-пользователя для проверки операций.',
    [isRegister],
  );

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLocalError(null);

    const normalizedEmail = email.trim();
    if (!normalizedEmail || !password) {
      setLocalError('Введите почту и пароль.');
      return;
    }

    if (password.length < 8) {
      setLocalError('Пароль должен быть не короче 8 символов.');
      return;
    }

    if (isRegister) {
      await onRegister(normalizedEmail, password);
    } else {
      await onLogin(normalizedEmail, password);
    }
  }

  async function handleDemoLogin(user: DemoUser) {
    setLocalError(null);
    setEmail(user.email);
    setPassword(user.password);
    await onDemoLogin(user);
  }

  return (
    <main className="auth-shell">
      <section className="auth-hero-card" aria-label="МИК Банк">
        <div className="auth-hero-card__brand">
          <span className="auth-logo">М</span>
          <span>
            <strong>МИК Банк</strong>
            <small>личный кабинет</small>
          </span>
        </div>

        <div className="auth-hero-card__content">
          <p className="eyebrow">Ваши финансы</p>
          <h1>Банк всегда рядом</h1>
          <p>Управляйте счетами, переводами и валютой в едином пространстве.</p>
        </div>

        <div className="auth-preview-card auth-preview-card--main">
          <span>Баланс</span>
          <strong>₽ 150 000</strong>
          <small>Демо-счёт</small>
        </div>
        <div className="auth-preview-card auth-preview-card--small">
          <span>Курс USD</span>
          <strong>90.00 ₽</strong>
        </div>
      </section>

      <section className="auth-panel" aria-label="Авторизация">
        <div className="auth-panel__top">
          <span className="auth-badge">Безопасный вход</span>
          <h2>{title}</h2>
          <p>{subtitle}</p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            <span>Почта</span>
            <input
              type="email"
              autoComplete="email"
              value={email}
              placeholder="igor@example.com"
              onChange={(event) => setEmail(event.target.value)}
              disabled={loading}
            />
          </label>

          <label>
            <span>Пароль</span>
            <input
              type="password"
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              value={password}
              placeholder="Введите пароль"
              onChange={(event) => setPassword(event.target.value)}
              disabled={loading}
            />
          </label>

          {(localError || error) && <div className="auth-error">{localError || error}</div>}

          <button className="auth-submit" type="submit" disabled={loading}>
            {loading ? 'Проверяем данные…' : isRegister ? 'Зарегистрироваться' : 'Войти'}
          </button>
        </form>

        <div className="auth-demo-list" aria-label="Демо-пользователи">
          <span>Демо-пользователи</span>
          {DEMO_USERS.map((user) => (
            <button
              className="auth-demo-button"
              key={user.email}
              type="button"
              onClick={() => void handleDemoLogin(user)}
              disabled={loading}
            >
              Войти как {user.name}
            </button>
          ))}
        </div>

        <div className="auth-switch">
          <span>{isRegister ? 'Уже есть аккаунт?' : 'Нет аккаунта?'}</span>
          <button
            type="button"
            onClick={() => {
              setLocalError(null);
              setMode(isRegister ? 'login' : 'register');
            }}
            disabled={loading}
          >
            {isRegister ? 'Войти' : 'Создать аккаунт'}
          </button>
        </div>
      </section>
    </main>
  );
}

export default AuthPage;
