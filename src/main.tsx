import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './app/App';
import './styles/index.css';

const root = document.getElementById('root');
if (!root) {
  document.body.innerHTML = '<h1 style="color:red;padding:20px">Root element not found</h1>';
} else {
  try {
    createRoot(root).render(
      <React.StrictMode>
        <App />
      </React.StrictMode>
    );
  } catch (error) {
    root.innerHTML = `<h1 style="color:red;padding:20px">Error: ${String(error)}</h1><pre>${JSON.stringify(error, null, 2)}</pre>`;
  }
}
