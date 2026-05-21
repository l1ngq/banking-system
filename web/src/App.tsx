import { useState } from 'react';
import './App.css';
import ActionModal from './components/ActionModal';
import Dashboard from './components/Dashboard';
import Footer from './components/Footer';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import ToastStack from './components/ToastStack';
import { useBankingState } from './hooks/useBankingState';
import AccountsPage from './pages/AccountsPage';
import AuthPage from './pages/AuthPage';
import CurrencyPage from './pages/CurrencyPage';
import PaymentsPage from './pages/PaymentsPage';
import ProfilePage from './pages/ProfilePage';
import type { Action, Page } from './types/banking';

function App() {
  const [activePage, setActivePage] = useState<Page>('dashboard');
  const [activeAction, setActiveAction] = useState<Action | null>(null);
  const banking = useBankingState();
  const { auth, state } = banking;

  function renderPage() {
    switch (activePage) {
      case 'dashboard':
        return <Dashboard state={state} summary={banking.summary} onAction={setActiveAction} onNavigate={setActivePage} />;
      case 'accounts':
        return <AccountsPage accounts={state.accounts} onAction={setActiveAction} onCloseAccount={banking.closeAccount} />;
      case 'payments':
        return <PaymentsPage accounts={state.accounts} transactions={state.transactions} onAction={setActiveAction} />;
      case 'currency':
        return <CurrencyPage rates={state.rates} accounts={state.accounts} onAction={setActiveAction} />;
      case 'profile':
        return <ProfilePage profile={state.profile} goals={state.goals} cashbackCategories={state.cashbackCategories} onUpdate={banking.updateProfile} />;
      default:
        return null;
    }
  }

  if (auth.status !== 'authenticated') {
    return (
      <>
        <AuthPage
          loading={auth.status === 'checking'}
          error={auth.error}
          onLogin={banking.login}
          onRegister={banking.register}
        />
        <ToastStack toasts={banking.toasts} onRemove={banking.removeToast} />
      </>
    );
  }

  return (
    <div className="app">
      <Sidebar activePage={activePage} onNavigate={setActivePage} />
      <main className="main">
        <Header
          activePage={activePage}
          theme={state.profile.theme}
          userName={state.profile.fullName}
          onLogout={banking.logout}
          onProfileOpen={() => setActivePage('profile')}
          onThemeToggle={() => banking.setTheme(state.profile.theme === 'dark' ? 'light' : 'dark')}
        />
        <section className="content">{renderPage()}<Footer onNavigate={setActivePage} /></section>
      </main>
      <ActionModal action={activeAction} accounts={state.accounts} onClose={() => setActiveAction(null)} onSubmit={banking.submitAction} />
      <ToastStack toasts={banking.toasts} onRemove={banking.removeToast} />
    </div>
  );
}

export default App;
