import { FormEvent, useMemo, useState } from 'react';

type AuthMode = 'login' | 'register';

interface AuthPageProps {
  loading?: boolean;
  error?: string | null;
  onLogin: (email: string, password: string) => Promise<void>;
  onRegister: (email: string, password: string) => Promise<void>;
}

function AuthPage({ loading = false, error, onLogin, onRegister }: AuthPageProps) {
  const [mode, setMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);

  const isRegister = mode === 'register';

  const title = useMemo(
    () => (isRegister ? 'Создайте аккаунт' : 'Войдите в МИК Банк'),
    [isRegister],
  );

  const subtitle = useMemo(
    () =>
      isRegister
        ? 'Укажите почту и пароль, чтобы открыть новый личный кабинет.'
        : 'Введите данные своего аккаунта для входа в личный кабинет.',
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

    if (!normalizedEmail.includes('@')) {
      setLocalError('Введите корректную почту.');
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

  function switchMode(nextMode: AuthMode) {
    setMode(nextMode);
    setLocalError(null);
  }

  return (
    <main className="auth-shell">
      <div className="auth-layout">
        <section className="auth-hero" aria-label="МИК Банк">
          <div className="auth-hero__top">
            <div className="auth-brand">
              <div className="auth-brand__logo">М</div>
              <div>
                <strong>МИК Банк</strong>
                <span>личный кабинет</span>
              </div>
            </div>
          </div>

          <div className="auth-hero__content">
            <p className="auth-hero__eyebrow">онлайн-банк</p>

            <h1 className="auth-hero__title">
              Управляйте счетами
              <br />
              просто и удобно
            </h1>

            <p className="auth-hero__subtitle">
              В одном кабинете доступны счета, переводы, операции и работа с валютой.
              После регистрации вы получаете новый пустой личный кабинет.
            </p>

            <div className="auth-hero__features">
              <article className="auth-feature-card">
                <span className="auth-feature-card__label">Счета</span>
                <strong>Открытие и управление</strong>
                <p>Создавайте счета, просматривайте баланс и выполняйте основные операции.</p>
              </article>

              <article className="auth-feature-card">
                <span className="auth-feature-card__label">Переводы</span>
                <strong>Быстрые операции</strong>
                <p>Переводите деньги между своими счетами и отслеживайте историю операций.</p>
              </article>

              <article className="auth-feature-card">
                <span className="auth-feature-card__label">Валюта</span>
                <strong>RUB · USD · EUR</strong>
                <p>Работайте с основными валютами и выполняйте обмен внутри кабинета.</p>
              </article>
            </div>
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
                placeholder="Введите почту"
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
              {loading ? 'Подождите…' : isRegister ? 'Создать аккаунт' : 'Войти'}
            </button>
          </form>

          <div className="auth-switch">
            {isRegister ? (
              <>
                Уже есть аккаунт?{' '}
                <button type="button" onClick={() => switchMode('login')} disabled={loading}>
                  Войти
                </button>
              </>
            ) : (
              <>
                Нет аккаунта?{' '}
                <button type="button" onClick={() => switchMode('register')} disabled={loading}>
                  Создать
                </button>
              </>
            )}
          </div>
        </section>
      </div>
    </main>
  );
}

export default AuthPage;
