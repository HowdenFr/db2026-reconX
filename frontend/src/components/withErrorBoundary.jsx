// TICKET-ADV113 — withErrorBoundary HOC: wraps a component in an error boundary.
import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    // In production this would be sent to a browser-side error service.
    // eslint-disable-next-line no-console
    console.error('ErrorBoundary caught', error, info);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      const { fallback: Fallback } = this.props;

      if (Fallback) {
        return <Fallback error={this.state.error} onReset={this.handleReset} />;
      }

      return (
        <div role="alert" className="error-fallback">
          <h2>Something went wrong</h2>
          <pre>{String(this.state.error?.message || this.state.error)}</pre>
          <button type="button" onClick={this.handleReset}>Try again</button>
        </div>
      );
    }

    return this.props.children;
  }
}

export function withErrorBoundary(Component, Fallback) {
  function WithErrorBoundary(props) {
    return (
      <ErrorBoundary fallback={Fallback}>
        <Component {...props} />
      </ErrorBoundary>
    );
  }
  WithErrorBoundary.displayName = `withErrorBoundary(${Component.displayName || Component.name || 'Component'})`;
  return WithErrorBoundary;
}
