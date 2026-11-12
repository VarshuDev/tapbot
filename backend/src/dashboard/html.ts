/**
 * Web-Based Admin Dashboard SPA for TapBot Store Management
 * Embedded Single Page Application served directly by Cloudflare Worker
 * Redesigned with ui-ux-pro-max Light Theme Design System
 */

export function renderDashboardHtml(nonce = ''): string {
    const safeNonce = nonce.replace(/[^A-Za-z0-9+/=_-]/g, '');
    const nonceAttr = safeNonce ? ` nonce="${safeNonce}"` : '';
    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TapBot Store Admin</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
    <style>
        :root {
            /* ui-ux-pro-max Light Theme Tokens */
            --bg-base: #f8fafc;
            --bg-surface: #ffffff;
            --bg-surface-hover: #f1f5f9;
            --bg-subtle: #f8fafc;
            --bg-card: #ffffff;
            --border-subtle: #e2e8f0;
            --border-prominent: #cbd5e1;
            --border-focus: #4f46e5;
            --text-main: #0f172a;
            --text-muted: #475569;
            --text-dim: #64748b;
            
            /* Primary Accents */
            --accent-indigo: #4f46e5;
            --accent-indigo-hover: #4338ca;
            --accent-indigo-light: #eef2ff;
            --accent-indigo-subtle: rgba(79, 70, 229, 0.08);
            --accent-cyan: #0284c7;
            --accent-cyan-light: #f0f9ff;
            --accent-purple: #7c3aed;
            
            /* Status Indicators (Accessible contrast >= 4.5:1) */
            --status-published: #047857;
            --status-published-bg: #ecfdf5;
            --status-published-border: #a7f3d0;
            
            --status-draft: #475569;
            --status-draft-bg: #f1f5f9;
            --status-draft-border: #cbd5e1;
            
            --status-review: #b45309;
            --status-review-bg: #fffbeb;
            --status-review-border: #fde68a;
            
            --status-unpublished: #6d28d9;
            --status-unpublished-bg: #f5f3ff;
            --status-unpublished-border: #ddd6fe;
            
            --status-archived: #be123c;
            --status-archived-bg: #fff1f2;
            --status-archived-border: #fecdd3;
            
            /* Elevation & Shadows */
            --shadow-xs: 0 1px 2px 0 rgba(15, 23, 42, 0.05);
            --shadow-sm: 0 1px 3px 0 rgba(15, 23, 42, 0.07), 0 1px 2px -1px rgba(15, 23, 42, 0.05);
            --shadow-md: 0 4px 6px -1px rgba(15, 23, 42, 0.07), 0 2px 4px -2px rgba(15, 23, 42, 0.05);
            --shadow-lg: 0 10px 15px -3px rgba(15, 23, 42, 0.08), 0 4px 6px -4px rgba(15, 23, 42, 0.04);
            --shadow-xl: 0 20px 25px -5px rgba(15, 23, 42, 0.1), 0 8px 10px -6px rgba(15, 23, 42, 0.05);
            
            /* Radius */
            --radius-sm: 6px;
            --radius-md: 8px;
            --radius-lg: 12px;
            --radius-xl: 16px;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg-base);
            color: var(--text-main);
            min-height: 100vh;
            line-height: 1.5;
            background-image: 
                radial-gradient(circle at 10% 10%, rgba(79, 70, 229, 0.04) 0%, transparent 45%),
                radial-gradient(circle at 90% 90%, rgba(2, 132, 199, 0.03) 0%, transparent 45%);
            background-attachment: fixed;
            -webkit-font-smoothing: antialiased;
        }

        .mono {
            font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, Consolas, monospace;
        }

        /* Top Navbar */
        header {
            position: sticky;
            top: 0;
            z-index: 40;
            background: rgba(255, 255, 255, 0.92);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            border-bottom: 1px solid var(--border-subtle);
            padding: 0.85rem 2rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            box-shadow: var(--shadow-xs);
        }

        .nav-brand {
            display: flex;
            align-items: center;
            gap: 0.85rem;
        }

        .brand-badge {
            background: linear-gradient(135deg, #4f46e5, #0284c7);
            width: 36px;
            height: 36px;
            border-radius: var(--radius-md);
            display: flex;
            align-items: center;
            justify-content: center;
            color: #ffffff;
            box-shadow: 0 4px 10px rgba(79, 70, 229, 0.3);
            flex-shrink: 0;
        }

        .brand-badge svg {
            width: 20px;
            height: 20px;
        }

        .brand-title {
            font-weight: 700;
            font-size: 1.15rem;
            letter-spacing: -0.02em;
            display: flex;
            align-items: center;
            gap: 0.6rem;
            color: var(--text-main);
        }

        .brand-tag {
            font-size: 0.72rem;
            font-weight: 600;
            letter-spacing: 0.04em;
            padding: 0.18rem 0.55rem;
            border-radius: var(--radius-sm);
            background: var(--accent-indigo-light);
            color: var(--accent-indigo);
            border: 1px solid rgba(79, 70, 229, 0.2);
        }

        .nav-actions {
            display: flex;
            align-items: center;
            gap: 0.65rem;
        }

        /* Buttons */
        .btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 0.45rem;
            font-size: 0.85rem;
            font-weight: 600;
            padding: 0.52rem 0.95rem;
            border-radius: var(--radius-md);
            cursor: pointer;
            border: 1px solid transparent;
            transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
            text-decoration: none;
            user-select: none;
            line-height: 1.25;
        }

        .btn svg {
            flex-shrink: 0;
        }

        .btn-primary {
            background: var(--accent-indigo);
            color: #ffffff;
            box-shadow: 0 1px 3px rgba(79, 70, 229, 0.35);
        }
        .btn-primary:hover {
            background: var(--accent-indigo-hover);
            box-shadow: 0 2px 6px rgba(79, 70, 229, 0.45);
        }
        .btn-primary:active {
            transform: translateY(1px);
        }

        .btn-secondary {
            background: var(--bg-surface);
            color: var(--text-muted);
            border-color: var(--border-prominent);
            box-shadow: var(--shadow-xs);
        }
        .btn-secondary:hover {
            background: var(--bg-surface-hover);
            color: var(--text-main);
            border-color: #94a3b8;
        }
        .btn-secondary:active {
            transform: translateY(1px);
        }

        .btn-success {
            background: #059669;
            color: #ffffff;
            box-shadow: 0 1px 3px rgba(5, 150, 105, 0.3);
        }
        .btn-success:hover {
            background: #047857;
        }

        .btn-danger {
            background: #ffffff;
            color: var(--status-archived);
            border-color: var(--status-archived-border);
            box-shadow: var(--shadow-xs);
        }
        .btn-danger:hover {
            background: var(--status-archived-bg);
            border-color: var(--status-archived);
            color: #9f1239;
        }

        .btn-sm {
            padding: 0.35rem 0.65rem;
            font-size: 0.8rem;
            border-radius: var(--radius-sm);
        }

        .btn:disabled {
            opacity: 0.55;
            cursor: not-allowed;
            pointer-events: none;
        }

        /* Layout Container */
        .container {
            max-width: 1280px;
            margin: 0 auto;
            padding: 2rem 1.75rem 4rem 1.75rem;
        }

        /* Metrics Grid */
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
            gap: 1.25rem;
            margin-bottom: 2rem;
        }

        .metric-card {
            background: var(--bg-card);
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-lg);
            padding: 1.25rem;
            position: relative;
            overflow: hidden;
            box-shadow: var(--shadow-sm);
            transition: all 0.2s ease;
        }
        .metric-card:hover {
            border-color: var(--border-prominent);
            box-shadow: var(--shadow-md);
            transform: translateY(-2px);
        }

        .metric-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 0.5rem;
        }

        .metric-title {
            font-size: 0.78rem;
            font-weight: 600;
            color: var(--text-dim);
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        .metric-icon-wrap {
            width: 30px;
            height: 30px;
            border-radius: var(--radius-md);
            display: flex;
            align-items: center;
            justify-content: center;
            background: var(--bg-surface-hover);
            color: var(--text-muted);
        }

        .metric-value {
            font-size: 2rem;
            font-weight: 700;
            letter-spacing: -0.03em;
            color: var(--text-main);
            line-height: 1.2;
        }

        .metric-subtitle {
            font-size: 0.78rem;
            color: var(--text-dim);
            margin-top: 0.35rem;
        }

        .pulse-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            display: inline-block;
            background: var(--status-published);
            box-shadow: 0 0 0 3px rgba(4, 120, 87, 0.2);
            animation: pulseAnim 2s infinite;
        }

        @keyframes pulseAnim {
            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(4, 120, 87, 0.4); }
            70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(4, 120, 87, 0); }
            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(4, 120, 87, 0); }
        }

        /* Bot Management Section */
        .section-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 1.25rem;
            flex-wrap: wrap;
            gap: 1rem;
        }

        .section-title {
            font-size: 1.35rem;
            font-weight: 700;
            letter-spacing: -0.02em;
            color: var(--text-main);
        }

        .filters-toolbar {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            flex-wrap: wrap;
        }

        .filter-tabs {
            display: flex;
            background: #edf2f7;
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-md);
            padding: 3px;
        }

        .filter-tab {
            padding: 0.4rem 0.85rem;
            font-size: 0.8rem;
            font-weight: 600;
            color: var(--text-muted);
            border-radius: var(--radius-sm);
            cursor: pointer;
            transition: all 0.15s ease;
            user-select: none;
        }
        .filter-tab:hover {
            color: var(--text-main);
        }
        .filter-tab.active {
            background: var(--bg-surface);
            color: var(--accent-indigo);
            font-weight: 700;
            box-shadow: var(--shadow-xs);
        }

        .search-box {
            position: relative;
        }

        .search-input {
            background: var(--bg-surface);
            border: 1px solid var(--border-prominent);
            color: var(--text-main);
            padding: 0.5rem 0.85rem 0.5rem 2.2rem;
            border-radius: var(--radius-md);
            font-size: 0.85rem;
            outline: none;
            transition: all 0.15s ease;
            width: 230px;
            box-shadow: var(--shadow-xs);
        }
        .search-input:focus {
            border-color: var(--border-focus);
            box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
        }
        .search-input::placeholder {
            color: var(--text-dim);
        }

        .search-icon {
            position: absolute;
            left: 0.75rem;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-dim);
            pointer-events: none;
            display: flex;
            align-items: center;
        }

        /* Table Card */
        .table-card {
            background: var(--bg-card);
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-lg);
            overflow: hidden;
            box-shadow: var(--shadow-sm);
        }

        .table-responsive {
            overflow-x: auto;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            text-align: left;
            font-size: 0.875rem;
        }

        th {
            background: #f8fafc;
            padding: 0.85rem 1.15rem;
            font-weight: 600;
            font-size: 0.74rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--text-muted);
            border-bottom: 1px solid var(--border-subtle);
        }

        td {
            padding: 0.95rem 1.15rem;
            border-bottom: 1px solid #f1f5f9;
            vertical-align: middle;
            color: var(--text-main);
        }

        tr:last-child td {
            border-bottom: none;
        }

        tbody tr {
            transition: background-color 0.1s ease;
        }

        tbody tr:hover {
            background-color: #f8fafc;
        }

        .bot-cell {
            display: flex;
            align-items: center;
            gap: 0.85rem;
        }

        .bot-icon {
            width: 40px;
            height: 40px;
            border-radius: var(--radius-md);
            background: var(--bg-surface-hover);
            border: 1px solid var(--border-subtle);
            object-fit: cover;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            color: var(--accent-indigo);
            font-size: 1.1rem;
            flex-shrink: 0;
            box-shadow: var(--shadow-xs);
        }

        .bot-name {
            font-weight: 600;
            color: var(--text-main);
            margin-bottom: 0.15rem;
            font-size: 0.92rem;
        }

        .bot-slug {
            font-size: 0.75rem;
            color: var(--text-dim);
        }

        /* Badges */
        .badge {
            display: inline-flex;
            align-items: center;
            gap: 0.35rem;
            padding: 0.22rem 0.6rem;
            border-radius: 9999px;
            font-size: 0.72rem;
            font-weight: 600;
            letter-spacing: 0.03em;
            text-transform: uppercase;
        }

        .badge-published {
            background: var(--status-published-bg);
            color: var(--status-published);
            border: 1px solid var(--status-published-border);
        }

        .badge-draft {
            background: var(--status-draft-bg);
            color: var(--status-draft);
            border: 1px solid var(--status-draft-border);
        }

        .badge-review {
            background: var(--status-review-bg);
            color: var(--status-review);
            border: 1px solid var(--status-review-border);
        }

        .badge-unpublished {
            background: var(--status-unpublished-bg);
            color: var(--status-unpublished);
            border: 1px solid var(--status-unpublished-border);
        }

        .badge-archived {
            background: var(--status-archived-bg);
            color: var(--status-archived);
            border: 1px solid var(--status-archived-border);
        }

        .badge-tag {
            background: #f1f5f9;
            border: 1px solid var(--border-subtle);
            color: var(--text-muted);
            padding: 0.18rem 0.5rem;
            border-radius: var(--radius-sm);
            font-size: 0.74rem;
            font-weight: 500;
        }

        /* Action Buttons */
        .actions-cell {
            display: flex;
            align-items: center;
            gap: 0.45rem;
            flex-wrap: wrap;
        }

        /* Modals */
        .modal-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(15, 23, 42, 0.45);
            backdrop-filter: blur(4px);
            -webkit-backdrop-filter: blur(4px);
            z-index: 100;
            display: none;
            align-items: center;
            justify-content: center;
            padding: 1.25rem;
        }

        .modal-overlay.active {
            display: flex;
        }

        .modal-box {
            background: var(--bg-surface);
            border: 1px solid var(--border-prominent);
            border-radius: var(--radius-xl);
            width: 100%;
            max-width: 580px;
            max-height: 90vh;
            overflow-y: auto;
            box-shadow: var(--shadow-xl);
            display: flex;
            flex-direction: column;
            animation: modalFadeIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }

        @keyframes modalFadeIn {
            from { opacity: 0; transform: scale(0.97) translateY(8px); }
            to { opacity: 1; transform: scale(1) translateY(0); }
        }

        .modal-box-lg {
            max-width: 840px;
        }

        .modal-header {
            padding: 1.25rem 1.5rem;
            border-bottom: 1px solid var(--border-subtle);
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: var(--bg-surface);
        }

        .modal-title {
            font-size: 1.15rem;
            font-weight: 700;
            color: var(--text-main);
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .modal-close {
            background: transparent;
            border: none;
            color: var(--text-dim);
            font-size: 1.35rem;
            cursor: pointer;
            padding: 0.3rem;
            border-radius: var(--radius-sm);
            line-height: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.15s;
        }
        .modal-close:hover {
            color: var(--text-main);
            background: var(--bg-surface-hover);
        }

        .modal-body {
            padding: 1.5rem;
            flex: 1;
            overflow-y: auto;
        }

        .modal-footer {
            padding: 1rem 1.5rem;
            border-top: 1px solid var(--border-subtle);
            display: flex;
            align-items: center;
            justify-content: flex-end;
            gap: 0.75rem;
            background: #f8fafc;
            border-bottom-left-radius: var(--radius-xl);
            border-bottom-right-radius: var(--radius-xl);
        }

        /* Form Controls */
        .form-group {
            margin-bottom: 1.15rem;
        }

        .form-label {
            display: block;
            font-size: 0.82rem;
            font-weight: 600;
            color: var(--text-muted);
            margin-bottom: 0.4rem;
        }

        .form-control {
            width: 100%;
            background: var(--bg-surface);
            border: 1px solid var(--border-prominent);
            color: var(--text-main);
            padding: 0.55rem 0.85rem;
            border-radius: var(--radius-md);
            font-size: 0.875rem;
            outline: none;
            transition: all 0.15s ease;
            box-shadow: var(--shadow-xs);
        }
        .form-control:focus {
            border-color: var(--border-focus);
            box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
        }
        .form-control::placeholder {
            color: var(--text-dim);
        }

        .form-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 1rem;
        }

        textarea.form-control {
            min-height: 85px;
            resize: vertical;
        }

        /* File Upload Dropzone */
        .dropzone {
            border: 2px dashed var(--border-prominent);
            border-radius: var(--radius-lg);
            padding: 1.75rem;
            text-align: center;
            background: #f8fafc;
            cursor: pointer;
            transition: all 0.2s ease;
        }
        .dropzone:hover, .dropzone.dragover {
            border-color: var(--accent-indigo);
            background: var(--accent-indigo-light);
        }

        .dropzone-icon {
            font-size: 2rem;
            margin-bottom: 0.5rem;
            color: var(--accent-indigo);
            display: flex;
            justify-content: center;
        }

        .dropzone-text {
            font-size: 0.875rem;
            color: var(--text-muted);
        }

        .dropzone-subtext {
            font-size: 0.75rem;
            color: var(--text-dim);
            margin-top: 0.25rem;
        }

        /* Drawer (Audit Log) */
        .drawer {
            position: fixed;
            top: 0;
            right: -520px;
            width: 500px;
            max-width: 92vw;
            height: 100vh;
            background: var(--bg-surface);
            border-left: 1px solid var(--border-subtle);
            box-shadow: -8px 0 24px rgba(15, 23, 42, 0.08);
            z-index: 90;
            transition: right 0.25s cubic-bezier(0.16, 1, 0.3, 1);
            display: flex;
            flex-direction: column;
        }

        .drawer.active {
            right: 0;
        }

        .drawer-header {
            padding: 1.25rem 1.5rem;
            border-bottom: 1px solid var(--border-subtle);
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: var(--bg-surface);
        }

        .drawer-body {
            padding: 1.25rem;
            flex: 1;
            overflow-y: auto;
            background: #f8fafc;
        }

        .audit-item {
            padding: 1rem;
            background: var(--bg-surface);
            border: 1px solid var(--border-subtle);
            border-radius: var(--radius-md);
            margin-bottom: 0.85rem;
            font-size: 0.82rem;
            box-shadow: var(--shadow-xs);
        }

        .audit-meta {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 0.4rem;
        }

        .audit-action {
            font-weight: 700;
            color: var(--accent-indigo);
            text-transform: uppercase;
            font-size: 0.74rem;
            letter-spacing: 0.03em;
        }

        .audit-time {
            font-size: 0.72rem;
            color: var(--text-dim);
        }

        .audit-details {
            background: #f1f5f9;
            padding: 0.5rem;
            border-radius: var(--radius-sm);
            font-size: 0.74rem;
            color: var(--text-muted);
            overflow-x: auto;
            margin-top: 0.45rem;
            border: 1px solid #e2e8f0;
        }

        /* Toasts */
        .toast-container {
            position: fixed;
            bottom: 1.5rem;
            right: 1.5rem;
            z-index: 120;
            display: flex;
            flex-direction: column;
            gap: 0.6rem;
        }

        .toast {
            background: var(--bg-surface);
            border: 1px solid var(--border-prominent);
            border-radius: var(--radius-md);
            padding: 0.85rem 1.15rem;
            box-shadow: var(--shadow-lg);
            color: var(--text-main);
            font-size: 0.86rem;
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 0.7rem;
            animation: toastIn 0.2s ease-out;
            max-width: 380px;
        }
        .toast-success { border-left: 4px solid var(--status-published); }
        .toast-error { border-left: 4px solid var(--status-archived); }
        .toast-info { border-left: 4px solid var(--accent-indigo); }

        @keyframes toastIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }

        /* Empty State */
        .empty-state {
            padding: 3.5rem 1.5rem;
            text-align: center;
            color: var(--text-dim);
        }

        .empty-icon {
            font-size: 2.5rem;
            margin-bottom: 0.75rem;
            opacity: 0.5;
            display: flex;
            justify-content: center;
        }

        /* Dropdown Lifecycle Select */
        .lifecycle-select {
            background: var(--bg-surface);
            color: var(--text-main);
            border: 1px solid var(--border-prominent);
            border-radius: var(--radius-sm);
            padding: 0.35rem 0.6rem;
            font-size: 0.75rem;
            font-weight: 600;
            cursor: pointer;
            outline: none;
            transition: all 0.15s ease;
            box-shadow: var(--shadow-xs);
        }
        .lifecycle-select:focus {
            border-color: var(--border-focus);
            box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.15);
        }

        /* Responsive */
        @media (max-width: 768px) {
            header { padding: 0.75rem 1rem; }
            .container { padding: 1.25rem 1rem 3rem 1rem; }
            .form-row { grid-template-columns: 1fr; }
            .section-header { flex-direction: column; align-items: flex-start; }
            .filters-toolbar { width: 100%; justify-content: space-between; }
            .search-input { width: 100%; }
        }
    </style>
</head>
<body>

    <!-- Header Navigation -->
    <header>
        <div class="nav-brand">
            <div class="brand-badge" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/>
                </svg>
            </div>
            <div>
                <div class="brand-title">
                    TapBot Store Admin
                    <span class="brand-tag">Cloudflare D1 & R2</span>
                </div>
            </div>
        </div>
        <div class="nav-actions">
            <button id="btn-refresh-dashboard" class="btn btn-secondary btn-sm" title="Refresh store data">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/>
                </svg>
                Refresh
            </button>
            <button id="btn-toggle-audit" class="btn btn-secondary btn-sm" title="View admin audit trail">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
                Audit Trail
            </button>
            <button id="btn-logout" class="btn btn-danger btn-sm" title="Sign out of admin session">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
                Logout
            </button>
        </div>
    </header>

    <!-- Main Container -->
    <main class="container">

        <!-- Top Metrics Bento Grid -->
        <section class="metrics-grid">
            <div class="metric-card">
                <div class="metric-header">
                    <div class="metric-title">Total Catalog</div>
                    <div class="metric-icon-wrap">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                            <line x1="8" y1="21" x2="16" y2="21"/>
                            <line x1="12" y1="17" x2="12" y2="21"/>
                        </svg>
                    </div>
                </div>
                <div class="metric-value mono" id="stat-total-bots">-</div>
                <div class="metric-subtitle">Across all lifecycle states</div>
            </div>

            <div class="metric-card">
                <div class="metric-header">
                    <div class="metric-title">Published Bots</div>
                    <span class="pulse-dot" title="Live on production store"></span>
                </div>
                <div class="metric-value mono" style="color: var(--status-published);" id="stat-published-bots">-</div>
                <div class="metric-subtitle">Visible to Android clients</div>
            </div>

            <div class="metric-card">
                <div class="metric-header">
                    <div class="metric-title">Drafts & Review</div>
                    <div class="metric-icon-wrap" style="color: var(--status-review); background: var(--status-review-bg);">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="10"/>
                            <polyline points="12 6 12 12 16 14"/>
                        </svg>
                    </div>
                </div>
                <div class="metric-value mono" style="color: var(--status-review);" id="stat-draft-bots">-</div>
                <div class="metric-subtitle">Pending publisher release</div>
            </div>

            <div class="metric-card">
                <div class="metric-header">
                    <div class="metric-title">Total Versions</div>
                    <div class="metric-icon-wrap" style="color: var(--accent-indigo); background: var(--accent-indigo-light);">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="16.5" y1="9.4" x2="7.5" y2="4.21"/>
                            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                            <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                            <line x1="12" y1="22.08" x2="12" y2="12"/>
                        </svg>
                    </div>
                </div>
                <div class="metric-value mono" style="color: var(--accent-indigo);" id="stat-total-versions">-</div>
                <div class="metric-subtitle">Packaged bot releases</div>
            </div>

            <div class="metric-card">
                <div class="metric-header">
                    <div class="metric-title">Total Installs</div>
                    <div class="metric-icon-wrap" style="color: var(--accent-cyan); background: var(--accent-cyan-light);">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                            <polyline points="7 10 12 15 17 10"/>
                            <line x1="12" y1="15" x2="12" y2="3"/>
                        </svg>
                    </div>
                </div>
                <div class="metric-value mono" style="color: var(--accent-cyan);" id="stat-downloads">Not available</div>
                <div class="metric-subtitle">Total bot downloads across catalog</div>
            </div>
        </section>

        <!-- Bot Management Section -->
        <section>
            <div class="section-header">
                <div>
                    <h2 class="section-title">Bot Catalog Management</h2>
                    <p style="font-size: 0.85rem; color: var(--text-dim); margin-top: 0.2rem;">
                        Create, package, configure credentials, and publish Telegram bots to the Bot Store.
                    </p>
                </div>
                <div class="filters-toolbar">
                    <div class="filter-tabs">
                        <div class="filter-tab active" data-filter="all">All</div>
                        <div class="filter-tab" data-filter="published">Published</div>
                        <div class="filter-tab" data-filter="draft">Draft</div>
                        <div class="filter-tab" data-filter="review">Review</div>
                        <div class="filter-tab" data-filter="unpublished">Unpublished</div>
                        <div class="filter-tab" data-filter="archived">Archived</div>
                    </div>
                    <div class="search-box">
                        <span class="search-icon" aria-hidden="true">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="11" cy="11" r="8"/>
                                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                            </svg>
                        </span>
                        <input type="text" id="search-input" class="search-input" placeholder="Search bots...">
                    </div>
                    <button id="btn-create-bot" class="btn btn-primary">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="12" y1="5" x2="12" y2="19"/>
                            <line x1="5" y1="12" x2="19" y2="12"/>
                        </svg>
                        Create Bot
                    </button>
                </div>
            </div>

            <!-- Bot Catalog Table -->
            <div class="table-card">
                <div class="table-responsive">
                    <table id="bots-table">
                        <thead>
                            <tr>
                                <th>Bot</th>
                                <th>Category</th>
                                <th>Runtime</th>
                                <th>Version</th>
                                <th>Downloads</th>
                                <th>Rating</th>
                                <th>Lifecycle State</th>
                                <th>Credentials</th>
                                <th style="text-align: right;">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="bots-table-body">
                            <tr>
                                <td colspan="9" class="empty-state">
                                    <div class="empty-icon">⏳</div>
                                    <div>Loading bot store records...</div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </section>
    </main>

    <!-- ================================================================= -->
    <!-- MODALS & DRAWERS                                                  -->
    <!-- ================================================================= -->

    <!-- 1. Admin Authentication Modal (zero secrets in source) -->
    <div id="auth-modal" class="modal-overlay">
        <div class="modal-box" style="max-width: 440px;">
            <div class="modal-header">
                <div class="modal-title">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: var(--accent-indigo);">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    Publisher Authentication
                </div>
            </div>
            <div class="modal-body">
                <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1.25rem;">
                    Enter your Cloudflare Worker Admin API Key to access the Bot Store dashboard. Key is stored securely in your session storage.
                </p>
                <div class="form-group">
                    <label class="form-label" for="auth-key-input">Admin API Key</label>
                    <input type="password" id="auth-key-input" class="form-control mono" placeholder="Enter API secret key" autocomplete="current-password">
                </div>
                <div id="auth-error" style="color: var(--status-archived); font-size: 0.82rem; display: none; margin-top: 0.5rem; font-weight: 500;"></div>
            </div>
            <div class="modal-footer">
                <button id="btn-auth-submit" class="btn btn-primary" style="width: 100%;">
                    Authorize & Enter Dashboard
                </button>
            </div>
        </div>
    </div>

    <!-- 2. Create / Edit Bot Modal -->
    <div id="bot-modal" class="modal-overlay">
        <div class="modal-box">
            <div class="modal-header">
                <div class="modal-title" id="bot-modal-title">Create Bot</div>
                <button class="modal-close" data-close-modal="bot-modal" aria-label="Close modal">&times;</button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="bot-form-id">
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Bot Name *</label>
                        <input type="text" id="bot-form-name" class="form-control" placeholder="e.g. Ping Pong Runner">
                    </div>
                    <div class="form-group">
                        <label class="form-label">Slug (Unique ID) *</label>
                        <input type="text" id="bot-form-slug" class="form-control mono" placeholder="e.g. ping-pong-bot">
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label">Category *</label>
                        <select id="bot-form-category" class="form-control">
                            <option value="utilities">Utilities</option>
                            <option value="productivity">Productivity</option>
                            <option value="media">Media & Music</option>
                            <option value="ai">AI & Chat</option>
                            <option value="fun">Fun & Games</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Runtime *</label>
                        <select id="bot-form-runtime" class="form-control mono">
                            <option value="native_art">native_art (Android ART Coroutines)</option>
                            <option value="native_go">native_go (Compiled ELF)</option>
                            <option value="python">python (Chaquo/Chaquopy)</option>
                            <option value="node">node (NodeJS)</option>
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label">Short Description *</label>
                    <input type="text" id="bot-form-description" class="form-control" placeholder="Brief summary of what this bot does">
                </div>
                <div class="form-group">
                    <label class="form-label">Long Description (Optional)</label>
                    <textarea id="bot-form-long-description" class="form-control" placeholder="Detailed user guide and documentation"></textarea>
                </div>
                <div class="form-group">
                    <label class="form-label">Bot Icon</label>
                    <div style="display: flex; gap: 0.5rem; margin-bottom: 0.5rem;">
                        <input type="text" id="bot-form-icon-url" class="form-control" placeholder="Icon URL (e.g. https://... or upload below)">
                        <button type="button" id="btn-upload-icon" class="btn btn-secondary btn-sm">Upload File</button>
                        <input type="file" id="icon-file-input" accept="image/*" style="display: none;">
                    </div>
                    <div id="icon-preview-wrap" style="display: none; align-items: center; gap: 0.6rem;">
                        <img id="icon-preview-img" src="" alt="Icon preview" style="width: 36px; height: 36px; border-radius: var(--radius-sm); object-fit: cover; border: 1px solid var(--border-subtle);">
                        <span style="font-size: 0.78rem; color: var(--status-published); font-weight: 600;">✓ Icon uploaded to storage successfully</span>
                    </div>
                </div>
                <div class="form-group" id="bot-form-status-group">
                    <label class="form-label">Lifecycle Status</label>
                    <select id="bot-form-status" class="form-control mono">
                        <option value="draft">DRAFT (Hidden from store)</option>
                        <option value="review">REVIEW (Under staging testing)</option>
                        <option value="published">PUBLISHED (Live on Android App)</option>
                        <option value="unpublished">UNPUBLISHED (Temporarily disabled)</option>
                        <option value="archived">ARCHIVED (Deprecated)</option>
                    </select>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" data-close-modal="bot-modal">Cancel</button>
                <button id="btn-save-bot" class="btn btn-primary">Save Bot</button>
            </div>
        </div>
    </div>

    <!-- 3. Version Management Modal -->
    <div id="version-modal" class="modal-overlay">
        <div class="modal-box modal-box-lg">
            <div class="modal-header">
                <div class="modal-title" id="version-modal-title">Package & Version Management</div>
                <button class="modal-close" data-close-modal="version-modal" aria-label="Close modal">&times;</button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="ver-bot-id">

                <!-- Existing Versions List -->
                <h4 style="font-size: 0.95rem; font-weight: 700; margin-bottom: 0.75rem; color: var(--text-main);">Existing Releases</h4>
                <div class="table-responsive" style="margin-bottom: 1.5rem; max-height: 220px; border: 1px solid var(--border-subtle); border-radius: var(--radius-md);">
                    <table>
                        <thead>
                            <tr>
                                <th>Version</th>
                                <th>Status</th>
                                <th>Size</th>
                                <th>SHA-256 Checksum</th>
                                <th>Released</th>
                                <th style="text-align: right;">Action</th>
                            </tr>
                        </thead>
                        <tbody id="version-list-body">
                            <tr><td colspan="6" class="empty-state">Loading versions...</td></tr>
                        </tbody>
                    </table>
                </div>

                <!-- Create / Upload Version Form -->
                <div style="background: #f8fafc; border: 1px solid var(--border-prominent); border-radius: var(--radius-lg); padding: 1.35rem;">
                    <h4 style="font-size: 0.95rem; font-weight: 700; margin-bottom: 1rem; color: var(--accent-indigo); display: flex; align-items: center; gap: 0.5rem;">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="16.5" y1="9.4" x2="7.5" y2="4.21"/>
                            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                            <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                            <line x1="12" y1="22.08" x2="12" y2="12"/>
                        </svg>
                        Upload New Release Package
                    </h4>

                    <!-- Drag & Drop Package Zone -->
                    <div class="dropzone" id="pkg-dropzone">
                        <div class="dropzone-icon">
                            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                                <polyline points="7 10 12 15 17 10"/>
                                <line x1="12" y1="15" x2="12" y2="3"/>
                            </svg>
                        </div>
                        <div class="dropzone-text" id="dropzone-label">Click or drag & drop <strong>.botpkg</strong> or <strong>.zip</strong> archive here</div>
                        <div class="dropzone-subtext">Automatic package size verification and Web Crypto SHA-256 calculation</div>
                        <input type="file" id="pkg-file-input" accept=".botpkg,.zip" style="display: none;">
                    </div>

                    <div style="margin-top: 1.25rem;">
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">Version String *</label>
                                <input type="text" id="ver-input-version" class="form-control mono" placeholder="e.g. 1.0.0 or 1.1.0">
                            </div>
                            <div class="form-group">
                                <label class="form-label">Package Size (Bytes)</label>
                                <input type="number" id="ver-input-size" class="form-control mono" placeholder="Auto-calculated" readonly>
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Package Key (R2 Storage Key)</label>
                            <input type="text" id="ver-input-key" class="form-control mono" placeholder="Auto-filled upon upload" readonly>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Package SHA-256 Checksum</label>
                            <input type="text" id="ver-input-sha256" class="form-control mono" placeholder="Auto-calculated Web Crypto hash" readonly>
                        </div>

                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">Min App Version</label>
                                <input type="number" id="ver-input-min-app" class="form-control mono" value="1">
                            </div>
                            <div class="form-group">
                                <label class="form-label">Min Runtime Version</label>
                                <input type="text" id="ver-input-min-runtime" class="form-control mono" value="1.0.0">
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Release Notes</label>
                            <textarea id="ver-input-notes" class="form-control" placeholder="What's new in this version?"></textarea>
                        </div>

                        <div class="form-group">
                            <label class="form-label">Release Status</label>
                            <select id="ver-input-status" class="form-control mono">
                                <option value="published">Published (Make current version)</option>
                                <option value="unpublished">Unpublished (Hold release)</option>
                            </select>
                        </div>

                        <button class="btn btn-primary" style="width: 100%;" id="btn-submit-version" disabled>
                            Upload & Register Version
                        </button>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" data-close-modal="version-modal">Close</button>
            </div>
        </div>
    </div>

    <!-- 4. Credential Management Modal -->
    <div id="credential-modal" class="modal-overlay">
        <div class="modal-box modal-box-lg">
            <div class="modal-header">
                <div class="modal-title" id="credential-modal-title">Configure Required Credentials</div>
                <button class="modal-close" data-close-modal="credential-modal" aria-label="Close modal">&times;</button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="cred-bot-id">
                <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1.15rem;">
                    Specify tokens, API secrets, or configuration fields required by this bot before launch on the Android client.
                </p>

                <div class="table-responsive" style="margin-bottom: 1.5rem; border: 1px solid var(--border-subtle); border-radius: var(--radius-md);">
                    <table>
                        <thead>
                            <tr>
                                <th>Key</th>
                                <th>Display Name</th>
                                <th>Description</th>
                                <th>Required</th>
                                <th>Secret</th>
                                <th>Input Type</th>
                                <th style="text-align: right;">Action</th>
                            </tr>
                        </thead>
                        <tbody id="credential-list-body">
                            <tr><td colspan="7" class="empty-state">No credentials configured.</td></tr>
                        </tbody>
                    </table>
                </div>

                <!-- Add Credential Form -->
                <div style="background: #f8fafc; border: 1px solid var(--border-prominent); border-radius: var(--radius-lg); padding: 1.25rem;">
                    <h5 style="font-size: 0.92rem; font-weight: 700; color: var(--text-main); margin-bottom: 0.85rem; display: flex; align-items: center; gap: 0.4rem;">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="12" y1="5" x2="12" y2="19"/>
                            <line x1="5" y1="12" x2="19" y2="12"/>
                        </svg>
                        Add New Credential Field
                    </h5>
                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">Key Name *</label>
                            <input type="text" id="new-cred-key" class="form-control mono" placeholder="e.g. bot_token">
                        </div>
                        <div class="form-group">
                            <label class="form-label">Display Name *</label>
                            <input type="text" id="new-cred-name" class="form-control" placeholder="e.g. Telegram Bot Token">
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="form-label">Help Text / Description</label>
                        <input type="text" id="new-cred-desc" class="form-control" placeholder="e.g. Obtained from @BotFather on Telegram">
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">Input UI Type</label>
                            <select id="new-cred-type" class="form-control">
                                <option value="password">Password / Secret Mask</option>
                                <option value="text">Single Line Text</option>
                                <option value="textarea">Multi-line Text</option>
                            </select>
                        </div>
                        <div class="form-group" style="display: flex; align-items: center; gap: 1.5rem; margin-top: 1.5rem;">
                            <label style="font-size: 0.85rem; font-weight: 600; display: flex; align-items: center; gap: 0.45rem; cursor: pointer; color: var(--text-main);">
                                <input type="checkbox" id="new-cred-required" checked> Required Field
                            </label>
                            <label style="font-size: 0.85rem; font-weight: 600; display: flex; align-items: center; gap: 0.45rem; cursor: pointer; color: var(--text-main);">
                                <input type="checkbox" id="new-cred-secret" checked> Secret Value
                            </label>
                        </div>
                    </div>
                    <button id="btn-add-credential" class="btn btn-primary btn-sm">
                        Save Credential Specification
                    </button>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" data-close-modal="credential-modal">Close</button>
            </div>
        </div>
    </div>

    <!-- 5. Audit Trail Drawer -->
    <div id="audit-drawer" class="drawer">
        <div class="drawer-header">
            <div style="font-weight: 700; font-size: 1.1rem; display: flex; align-items: center; gap: 0.5rem; color: var(--text-main);">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: var(--accent-indigo);">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                    <line x1="16" y1="13" x2="8" y2="13"/>
                    <line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
                Store Audit Trail
            </div>
            <button id="btn-close-audit" class="modal-close" aria-label="Close drawer">&times;</button>
        </div>
        <div class="drawer-body" id="audit-logs-list">
            <div class="empty-state">Loading audit trail...</div>
        </div>
    </div>

    <!-- Toast Notification Container -->
    <div id="toast-container" class="toast-container"></div>

    <!-- ================================================================= -->
    <!-- APPLICATION JAVASCRIPT                                            -->
    <!-- ================================================================= -->
    <script${nonceAttr}>
        const AUTH_STORAGE_KEY = 'tapbot_admin_key';
        let allBots = [];
        let currentFilter = 'all';

        // -----------------------------------------------------------------
        // Authentication & Session Management
        // -----------------------------------------------------------------
        function getAdminKey() {
            return sessionStorage.getItem(AUTH_STORAGE_KEY) || '';
        }

        function setAdminKey(key) {
            sessionStorage.setItem(AUTH_STORAGE_KEY, key);
        }

        function clearAdminKey() {
            sessionStorage.removeItem(AUTH_STORAGE_KEY);
        }

        function checkAuthOrPrompt() {
            const key = getAdminKey();
            if (!key) {
                document.getElementById('auth-modal').classList.add('active');
                return false;
            }
            return true;
        }

        async function handleAuthSubmit() {
            const input = document.getElementById('auth-key-input');
            const key = input.value.trim();
            const errorDiv = document.getElementById('auth-error');

            if (!key) {
                errorDiv.innerText = 'Please enter an admin API key';
                errorDiv.style.display = 'block';
                return;
            }

            try {
                // Test key against stats endpoint
                const res = await fetch('/api/v1/admin/stats', {
                    headers: { 'Authorization': 'Bearer ' + key }
                });

                if (res.status === 401) {
                    errorDiv.innerText = 'Unauthorized: Invalid Admin API Key';
                    errorDiv.style.display = 'block';
                    return;
                }

                if (!res.ok) {
                    throw new Error('Verification failed with status ' + res.status);
                }

                setAdminKey(key);
                document.getElementById('auth-modal').classList.remove('active');
                errorDiv.style.display = 'none';
                showToast('Authentication successful', 'success');
                loadDashboardData();
            } catch (err) {
                errorDiv.innerText = 'Error verifying key: ' + err.message;
                errorDiv.style.display = 'block';
            }
        }

        function handleLogout() {
            clearAdminKey();
            showToast('Logged out of admin session', 'info');
            document.getElementById('auth-modal').classList.add('active');
        }

        // -----------------------------------------------------------------
        // API Fetch Wrapper
        // -----------------------------------------------------------------
        async function apiCall(endpoint, options = {}) {
            const key = getAdminKey();
            if (!key) {
                checkAuthOrPrompt();
                throw new Error('Not authenticated');
            }

            options.headers = options.headers || {};
            if (!(options.body instanceof FormData)) {
                options.headers['Content-Type'] = options.headers['Content-Type'] || 'application/json';
            }
            options.headers['Authorization'] = 'Bearer ' + key;

            const res = await fetch(endpoint, options);

            if (res.status === 401) {
                clearAdminKey();
                checkAuthOrPrompt();
                throw new Error('Session expired or unauthorized');
            }

            const json = await res.json();
            if (!json.success) {
                throw new Error(json.error?.message || 'API request failed');
            }

            return json.data;
        }

        // -----------------------------------------------------------------
        // UI Helpers & Toasts
        // -----------------------------------------------------------------
        function showToast(message, type = 'info') {
            const container = document.getElementById('toast-container');
            const toast = document.createElement('div');
            toast.className = 'toast toast-' + type;
            const iconSvg = type === 'success' 
                ? '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#059669" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>'
                : (type === 'error'
                    ? '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#be123c" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>'
                    : '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#4f46e5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>');
            toast.innerHTML = iconSvg + '<span>' + escapeHtml(message) + '</span>';
            container.appendChild(toast);
            setTimeout(() => {
                toast.style.opacity = '0';
                setTimeout(() => toast.remove(), 200);
            }, 3500);
        }

        function escapeHtml(str) {
            if (!str) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;')
                .replace(/\`/g, '&#96;');
        }

        function sanitizeImageUrl(url) {
            if (!url) return '';
            const trimmed = String(url).trim();
            if (!trimmed) return '';
            if (trimmed.startsWith('/') && !trimmed.startsWith('//')) {
                return trimmed;
            }
            try {
                const parsed = new URL(trimmed);
                if (parsed.protocol === 'https:') {
                    return trimmed;
                }
                if (parsed.protocol === 'http:' && (parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1')) {
                    return trimmed;
                }
            } catch {
                return '';
            }
            return '';
        }

        function formatBytes(bytes) {
            if (bytes === 0) return '0 B';
            const k = 1024;
            const sizes = ['B', 'KB', 'MB', 'GB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
        }

        function formatDate(dateStr) {
            if (!dateStr) return '-';
            try {
                const d = new Date(dateStr);
                return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            } catch {
                return dateStr;
            }
        }

        function closeModal(modalId) {
            document.getElementById(modalId).classList.remove('active');
        }

        // -----------------------------------------------------------------
        // Dashboard Data Loading
        // -----------------------------------------------------------------
        async function loadDashboardData() {
            if (!checkAuthOrPrompt()) return;

            try {
                // 1. Fetch Stats
                const stats = await apiCall('/api/v1/admin/stats');
                document.getElementById('stat-total-bots').innerText = stats.totalBots;
                document.getElementById('stat-published-bots').innerText = stats.publishedBots;
                document.getElementById('stat-draft-bots').innerText = (stats.draftBots + stats.reviewBots);
                document.getElementById('stat-total-versions').innerText = stats.totalVersions;
                // Strictly display accurate measured counts without fake claims
                document.getElementById('stat-downloads').innerText = stats.downloads;

                // 2. Fetch Bots
                allBots = await apiCall('/api/v1/admin/bots');
                renderBotsTable();
            } catch (err) {
                console.error('Failed to load dashboard data:', err);
                showToast(err.message, 'error');
            }
        }

        // -----------------------------------------------------------------
        // Render Bots Table
        // -----------------------------------------------------------------
        function renderBotsTable() {
            const tbody = document.getElementById('bots-table-body');
            const search = (document.getElementById('search-input').value || '').trim().toLowerCase();

            const filtered = allBots.filter(bot => {
                const matchesFilter = (currentFilter === 'all') || (bot.status.toLowerCase() === currentFilter);
                const matchesSearch = !search ||
                    bot.name.toLowerCase().includes(search) ||
                    bot.slug.toLowerCase().includes(search) ||
                    bot.category.toLowerCase().includes(search);
                return matchesFilter && matchesSearch;
            });

            if (filtered.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" class="empty-state">' +
                    '<div class="empty-icon">' +
                        '<svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>' +
                    '</div>' +
                    '<div style="font-weight: 500;">No bots found matching criteria.</div>' +
                    '</td></tr>';
                return;
            }

            tbody.innerHTML = filtered.map(bot => {
                const status = (bot.status || 'draft').toLowerCase();
                const safeIconUrl = sanitizeImageUrl(bot.iconUrl);
                const iconHtml = safeIconUrl
                    ? '<img class="bot-icon" src="' + escapeHtml(safeIconUrl) + '" alt="' + escapeHtml(bot.name) + '">'
                    : '<div class="bot-icon"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="10" rx="2"/><circle cx="12" cy="5" r="2"/><path d="M12 7v4"/><line x1="8" y1="16" x2="8.01" y2="16"/><line x1="16" y1="16" x2="16.01" y2="16"/></svg></div>';

                const verText = bot.currentVersion
                    ? '<span class="mono" style="font-weight: 600; color: var(--accent-indigo);">v' + escapeHtml(bot.currentVersion) + '</span>'
                    : '<span style="color: var(--text-dim); font-size: 0.8rem;">None</span>';

                const credCount = bot.credentials ? bot.credentials.length : 0;
                const dlCount = (bot.downloadsCount || 0).toLocaleString();
                const ratingText = bot.rating ? Number(bot.rating).toFixed(1) : '5.0';

                return '<tr>' +
                    '<td>' +
                        '<div class="bot-cell">' +
                            iconHtml +
                            '<div>' +
                                '<div class="bot-name">' + escapeHtml(bot.name) + '</div>' +
                                '<div class="bot-slug mono">' + escapeHtml(bot.slug) + '</div>' +
                            '</div>' +
                        '</div>' +
                    '</td>' +
                    '<td><span class="badge-tag">' + escapeHtml(bot.category) + '</span></td>' +
                    '<td><span class="badge-tag mono">' + escapeHtml(bot.runtime || 'native_art') + '</span></td>' +
                    '<td>' + verText + '</td>' +
                    '<td><span class="mono" style="font-weight: 600; color: var(--accent-cyan); display: inline-flex; align-items: center; gap: 0.25rem;">' +
                        '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>' +
                        dlCount +
                    '</span></td>' +
                    '<td><span style="color: #d97706; font-weight: 600; display: inline-flex; align-items: center; gap: 0.2rem;">' +
                        '<svg width="13" height="13" viewBox="0 0 24 24" fill="#d97706" stroke="#d97706"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>' +
                        ratingText +
                    '</span></td>' +
                    '<td>' +
                        '<select class="lifecycle-select" data-action="change-lifecycle" data-bot-id="' + escapeHtml(bot.id) + '">' +
                            '<option value="DRAFT"' + (status === 'draft' ? ' selected' : '') + '>DRAFT</option>' +
                            '<option value="REVIEW"' + (status === 'review' ? ' selected' : '') + '>REVIEW</option>' +
                            '<option value="PUBLISHED"' + (status === 'published' ? ' selected' : '') + '>PUBLISHED</option>' +
                            '<option value="UNPUBLISHED"' + (status === 'unpublished' ? ' selected' : '') + '>UNPUBLISHED</option>' +
                            '<option value="ARCHIVED"' + (status === 'archived' ? ' selected' : '') + '>ARCHIVED</option>' +
                        '</select>' +
                    '</td>' +
                    '<td>' +
                        '<button class="btn btn-secondary btn-sm" data-action="open-credentials" data-bot-id="' + escapeHtml(bot.id) + '">' +
                            '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 2l-2 2m-1.5 1.5L14 9l-1.5-1.5-3 3 1.5 1.5L9 14l-1.5-1.5L6 14a6 6 0 1 1 8.49-8.49L21 2z"/></svg>' +
                            credCount + ' Fields' +
                        '</button>' +
                    '</td>' +
                    '<td style="text-align: right;">' +
                        '<div class="actions-cell" style="justify-content: flex-end;">' +
                            '<button class="btn btn-secondary btn-sm" data-action="open-versions" data-bot-id="' + escapeHtml(bot.id) + '" title="Manage releases and packages">' +
                                '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="16.5" y1="9.4" x2="7.5" y2="4.21"/><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>' +
                                'Releases' +
                            '</button>' +
                            '<button class="btn btn-secondary btn-sm" data-action="edit-bot" data-bot-id="' + escapeHtml(bot.id) + '" title="Edit bot metadata">' +
                                '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>' +
                            '</button>' +
                            '<button class="btn btn-danger btn-sm" data-action="delete-bot" data-bot-id="' + escapeHtml(bot.id) + '" data-bot-name="' + escapeHtml(bot.name) + '" title="Delete bot">' +
                                '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>' +
                            '</button>' +
                        '</div>' +
                    '</td>' +
                '</tr>';
            }).join('');
        }

        function setFilter(filter, el) {
            currentFilter = filter;
            document.querySelectorAll('.filter-tab').forEach(tab => tab.classList.remove('active'));
            el.classList.add('active');
            renderBotsTable();
        }

        function handleSearch() {
            renderBotsTable();
        }

        // -----------------------------------------------------------------
        // Bot CRUD
        // -----------------------------------------------------------------
        function openCreateBotModal() {
            document.getElementById('bot-modal-title').innerText = 'Create New Bot';
            document.getElementById('bot-form-id').value = '';
            document.getElementById('bot-form-name').value = '';
            document.getElementById('bot-form-slug').value = '';
            document.getElementById('bot-form-slug').readOnly = false;
            document.getElementById('bot-form-description').value = '';
            document.getElementById('bot-form-long-description').value = '';
            document.getElementById('bot-form-icon-url').value = '';
            document.getElementById('bot-form-category').value = 'utilities';
            document.getElementById('bot-form-runtime').value = 'native_art';
            document.getElementById('bot-form-status').value = 'draft';
            document.getElementById('icon-preview-wrap').style.display = 'none';

            document.getElementById('bot-modal').classList.add('active');
        }

        function handleNameSlugAutoFill() {
            const idField = document.getElementById('bot-form-id').value;
            if (!idField) {
                const name = document.getElementById('bot-form-name').value;
                const slug = name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
                document.getElementById('bot-form-slug').value = slug;
            }
        }

        function openEditBotModal(botId) {
            const bot = allBots.find(b => b.id === botId);
            if (!bot) return;

            document.getElementById('bot-modal-title').innerText = 'Edit Bot: ' + bot.name;
            document.getElementById('bot-form-id').value = bot.id;
            document.getElementById('bot-form-name').value = bot.name;
            document.getElementById('bot-form-slug').value = bot.slug;
            document.getElementById('bot-form-slug').readOnly = true;
            document.getElementById('bot-form-description').value = bot.description || '';
            document.getElementById('bot-form-long-description').value = bot.longDescription || '';
            document.getElementById('bot-form-icon-url').value = bot.iconUrl || '';
            document.getElementById('bot-form-category').value = bot.category || 'utilities';
            document.getElementById('bot-form-runtime').value = bot.runtime || 'native_art';
            document.getElementById('bot-form-status').value = bot.status || 'draft';

            const safeIconPreview = sanitizeImageUrl(bot.iconUrl);
            if (safeIconPreview) {
                document.getElementById('icon-preview-img').src = safeIconPreview;
                document.getElementById('icon-preview-wrap').style.display = 'flex';
            } else {
                document.getElementById('icon-preview-wrap').style.display = 'none';
            }

            document.getElementById('bot-modal').classList.add('active');
        }

        async function saveBotForm() {
            const id = document.getElementById('bot-form-id').value;
            const name = document.getElementById('bot-form-name').value.trim();
            const slug = document.getElementById('bot-form-slug').value.trim();
            const category = document.getElementById('bot-form-category').value;
            const runtime = document.getElementById('bot-form-runtime').value;
            const description = document.getElementById('bot-form-description').value.trim();
            const longDescription = document.getElementById('bot-form-long-description').value.trim();
            const iconUrl = document.getElementById('bot-form-icon-url').value.trim();
            const status = document.getElementById('bot-form-status').value;

            if (!name || !slug || !description) {
                showToast('Please fill in required fields: Name, Slug, Description', 'error');
                return;
            }

            try {
                if (!id) {
                    // Create
                    await apiCall('/api/v1/admin/bots', {
                        method: 'POST',
                        body: JSON.stringify({ name, slug, category, runtime, description, longDescription, iconUrl, status })
                    });
                    showToast('Bot created successfully', 'success');
                } else {
                    // Update
                    await apiCall('/api/v1/admin/bots/' + encodeURIComponent(id), {
                        method: 'PUT',
                        body: JSON.stringify({ name, category, runtime, description, longDescription, iconUrl, status })
                    });
                    showToast('Bot updated successfully', 'success');
                }

                closeModal('bot-modal');
                loadDashboardData();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }

        async function handleIconFileUpload(input) {
            const file = input.files[0];
            if (!file) return;

            const formData = new FormData();
            formData.append('file', file);

            try {
                showToast('Uploading icon to storage...', 'info');
                const data = await apiCall('/api/v1/admin/upload/icon', {
                    method: 'POST',
                    body: formData
                });

                document.getElementById('bot-form-icon-url').value = data.iconUrl;
                document.getElementById('icon-preview-img').src = data.iconUrl;
                document.getElementById('icon-preview-wrap').style.display = 'flex';
                showToast('Icon uploaded successfully', 'success');
            } catch (err) {
                showToast('Failed to upload icon: ' + err.message, 'error');
            }
        }

        async function changeLifecycle(botId, newStatus) {
            try {
                await apiCall('/api/v1/admin/bots/' + encodeURIComponent(botId) + '/lifecycle', {
                    method: 'POST',
                    body: JSON.stringify({ status: newStatus })
                });
                showToast('Lifecycle transitioned to ' + newStatus, 'success');
                loadDashboardData();
            } catch (err) {
                showToast(err.message, 'error');
                loadDashboardData();
            }
        }

        async function confirmDeleteBot(botId, botName) {
            if (!confirm('Are you sure you want to delete "' + botName + '"? This will permanently delete all versions, credentials, and packages.')) {
                return;
            }

            try {
                await apiCall('/api/v1/admin/bots/' + encodeURIComponent(botId), {
                    method: 'DELETE'
                });
                showToast('Bot deleted successfully', 'success');
                loadDashboardData();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }

        // -----------------------------------------------------------------
        // Version Management
        // -----------------------------------------------------------------
        async function openVersionModal(botId) {
            const bot = allBots.find(b => b.id === botId);
            if (!bot) return;

            document.getElementById('ver-bot-id').value = bot.id;
            document.getElementById('version-modal-title').innerText = 'Releases & Packages: ' + bot.name;

            // Reset form
            document.getElementById('ver-input-version').value = '';
            document.getElementById('ver-input-size').value = '';
            document.getElementById('ver-input-key').value = '';
            document.getElementById('ver-input-sha256').value = '';
            document.getElementById('ver-input-notes').value = '';
            document.getElementById('ver-input-min-app').value = '1';
            document.getElementById('ver-input-min-runtime').value = '1.0.0';
            document.getElementById('ver-input-status').value = 'published';
            document.getElementById('dropzone-label').innerHTML = 'Click or drag & drop <strong>.botpkg</strong> or <strong>.zip</strong> archive here';
            document.getElementById('btn-submit-version').disabled = true;

            document.getElementById('version-modal').classList.add('active');

            await loadVersionsForBot(bot.id);
        }

        async function loadVersionsForBot(botId) {
            const tbody = document.getElementById('version-list-body');
            tbody.innerHTML = '<tr><td colspan="6" class="empty-state">Loading versions...</td></tr>';

            try {
                const botDetail = await apiCall('/api/v1/admin/bots/' + encodeURIComponent(botId));
                const versions = botDetail.versions || [];

                if (versions.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No releases found for this bot. Upload a package below.</td></tr>';
                    return;
                }

                tbody.innerHTML = versions.map(v => {
                    const isPub = v.status === 'published';
                    const statusBadge = isPub
                        ? '<span class="badge badge-published">Published</span>'
                        : '<span class="badge badge-unpublished">Unpublished</span>';

                    const actionBtn = isPub
                        ? '<button class="btn btn-secondary btn-sm" data-action="toggle-version-publish" data-bot-id="' + escapeHtml(botId) + '" data-version="' + escapeHtml(v.version) + '" data-publish="false">Unpublish</button>'
                        : '<button class="btn btn-success btn-sm" data-action="toggle-version-publish" data-bot-id="' + escapeHtml(botId) + '" data-version="' + escapeHtml(v.version) + '" data-publish="true">Publish</button>';

                    return '<tr>' +
                        '<td class="mono" style="font-weight: 600; color: var(--accent-indigo);">v' + escapeHtml(v.version) + '</td>' +
                        '<td>' + statusBadge + '</td>' +
                        '<td class="mono">' + formatBytes(v.packageSize) + '</td>' +
                        '<td class="mono" style="font-size: 0.74rem; max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-dim);" title="' + escapeHtml(v.sha256) + '">' +
                            escapeHtml(v.sha256) +
                        '</td>' +
                        '<td>' + formatDate(v.publishedAt) + '</td>' +
                        '<td style="text-align: right;">' + actionBtn + '</td>' +
                    '</tr>';
                }).join('');
            } catch (err) {
                tbody.innerHTML = '<tr><td colspan="6" class="empty-state" style="color: var(--status-archived);">' + escapeHtml(err.message) + '</td></tr>';
            }
        }

        async function handlePackageFileSelect(input) {
            const file = input.files[0];
            if (!file) return;

            const dropzoneLabel = document.getElementById('dropzone-label');
            dropzoneLabel.innerHTML = '⏳ Uploading <strong>' + escapeHtml(file.name) + '</strong> (' + formatBytes(file.size) + ')...';

            const formData = new FormData();
            formData.append('file', file);

            try {
                const result = await apiCall('/api/v1/admin/upload/package', {
                    method: 'POST',
                    body: formData
                });

                document.getElementById('ver-input-key').value = result.packageKey;
                document.getElementById('ver-input-size').value = result.packageSize;
                document.getElementById('ver-input-sha256').value = result.sha256;

                // Auto extract version if formatted as name_1.0.0.botpkg
                const verMatch = file.name.match(/_(\\d+\\.\\d+\\.\\d+)/);
                if (verMatch && !document.getElementById('ver-input-version').value) {
                    document.getElementById('ver-input-version').value = verMatch[1];
                }

                dropzoneLabel.innerHTML = '✓ <strong>' + escapeHtml(file.name) + '</strong> uploaded (' + formatBytes(result.packageSize) + ')<br><span class="mono" style="font-size: 0.74rem; color: var(--text-dim);">SHA-256: ' + result.sha256.substring(0, 16) + '...</span>';
                document.getElementById('btn-submit-version').disabled = false;
                showToast('Package uploaded and hashed successfully', 'success');
            } catch (err) {
                dropzoneLabel.innerHTML = '✕ Upload failed: ' + escapeHtml(err.message);
                showToast('Package upload error: ' + err.message, 'error');
            }
        }

        async function submitNewVersion() {
            const botId = document.getElementById('ver-bot-id').value;
            const version = document.getElementById('ver-input-version').value.trim();
            const packageKey = document.getElementById('ver-input-key').value.trim();
            const packageSize = parseInt(document.getElementById('ver-input-size').value, 10);
            const sha256 = document.getElementById('ver-input-sha256').value.trim();
            const minimumAppVersion = parseInt(document.getElementById('ver-input-min-app').value, 10) || 1;
            const minimumRuntimeVersion = document.getElementById('ver-input-min-runtime').value.trim() || '1.0.0';
            const releaseNotes = document.getElementById('ver-input-notes').value.trim();
            const status = document.getElementById('ver-input-status').value;

            if (!version || !packageKey || isNaN(packageSize) || !sha256) {
                showToast('Please specify a version and upload a valid package file', 'error');
                return;
            }

            try {
                await apiCall('/api/v1/admin/bots/' + encodeURIComponent(botId) + '/versions', {
                    method: 'POST',
                    body: JSON.stringify({
                        version,
                        packageKey,
                        packageSize,
                        sha256,
                        minimumAppVersion,
                        minimumRuntimeVersion,
                        releaseNotes,
                        status
                    })
                });

                showToast('Version v' + version + ' registered successfully', 'success');
                await loadVersionsForBot(botId);
                loadDashboardData();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }

        async function toggleVersionPublish(botId, version, shouldPublish) {
            const action = shouldPublish ? 'publish' : 'unpublish';
            try {
                await apiCall('/api/v1/admin/bots/' + encodeURIComponent(botId) + '/versions/' + encodeURIComponent(version) + '/' + action, {
                    method: 'POST'
                });
                showToast('Version v' + version + ' ' + (shouldPublish ? 'published' : 'unpublished'), 'success');
                await loadVersionsForBot(botId);
                loadDashboardData();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }

        // -----------------------------------------------------------------
        // Credential Management
        // -----------------------------------------------------------------
        async function openCredentialModal(botId) {
            const bot = allBots.find(b => b.id === botId);
            if (!bot) return;

            document.getElementById('cred-bot-id').value = bot.id;
            document.getElementById('credential-modal-title').innerText = 'Required Credentials: ' + bot.name;

            // Reset add form
            document.getElementById('new-cred-key').value = '';
            document.getElementById('new-cred-name').value = '';
            document.getElementById('new-cred-desc').value = '';
            document.getElementById('new-cred-type').value = 'password';
            document.getElementById('new-cred-required').checked = true;
            document.getElementById('new-cred-secret').checked = true;

            document.getElementById('credential-modal').classList.add('active');
            await loadCredentialsForBot(bot.id);
        }

        async function loadCredentialsForBot(botId) {
            const tbody = document.getElementById('credential-list-body');
            tbody.innerHTML = '<tr><td colspan="7" class="empty-state">Loading credentials...</td></tr>';

            try {
                const botDetail = await apiCall('/api/v1/admin/bots/' + encodeURIComponent(botId));
                const creds = botDetail.credentials || [];

                if (creds.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No credentials configured for this bot. Add one below.</td></tr>';
                    return;
                }

                tbody.innerHTML = creds.map(c => {
                    return '<tr>' +
                        '<td class="mono" style="font-weight: 600; color: var(--accent-indigo);">' + escapeHtml(c.key) + '</td>' +
                        '<td style="font-weight: 500;">' + escapeHtml(c.displayName) + '</td>' +
                        '<td style="color: var(--text-dim);">' + escapeHtml(c.description || '-') + '</td>' +
                        '<td>' + (c.required ? '<span style="color: var(--status-published); font-weight: 600;">✓ Yes</span>' : '<span style="color: var(--text-dim);">No</span>') + '</td>' +
                        '<td>' + (c.secret ? '<span style="color: var(--accent-indigo); font-weight: 600;">🔒 Secret</span>' : '<span style="color: var(--text-dim);">Public</span>') + '</td>' +
                        '<td><span class="badge-tag">' + escapeHtml(c.inputType) + '</span></td>' +
                        '<td style="text-align: right;">' +
                            '<button class="btn btn-danger btn-sm" data-action="delete-credential" data-bot-id="' + escapeHtml(botId) + '" data-cred-key="' + escapeHtml(c.key) + '">Delete</button>' +
                        '</td>' +
                    '</tr>';
                }).join('');
            } catch (err) {
                tbody.innerHTML = '<tr><td colspan="7" class="empty-state" style="color: var(--status-archived);">' + escapeHtml(err.message) + '</td></tr>';
            }
        }

        async function addCredentialSubmit() {
            const botId = document.getElementById('cred-bot-id').value;
            const key = document.getElementById('new-cred-key').value.trim();
            const displayName = document.getElementById('new-cred-name').value.trim();
            const description = document.getElementById('new-cred-desc').value.trim();
            const inputType = document.getElementById('new-cred-type').value;
            const required = document.getElementById('new-cred-required').checked;
            const secret = document.getElementById('new-cred-secret').checked;

            if (!key || !displayName) {
                showToast('Please provide a key name and display name', 'error');
                return;
            }

            try {
                await apiCall('/api/v1/admin/bots/' + encodeURIComponent(botId) + '/credentials', {
                    method: 'POST',
                    body: JSON.stringify({ key, displayName, description, inputType, required, secret })
                });

                showToast('Credential ' + key + ' saved', 'success');
                document.getElementById('new-cred-key').value = '';
                document.getElementById('new-cred-name').value = '';
                document.getElementById('new-cred-desc').value = '';
                await loadCredentialsForBot(botId);
                loadDashboardData();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }

        async function deleteCredential(botId, key) {
            if (!confirm('Remove credential requirement "' + key + '"?')) return;

            try {
                await apiCall('/api/v1/admin/bots/' + encodeURIComponent(botId) + '/credentials/' + encodeURIComponent(key), {
                    method: 'DELETE'
                });
                showToast('Credential removed', 'success');
                await loadCredentialsForBot(botId);
                loadDashboardData();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }

        // -----------------------------------------------------------------
        // Audit Drawer
        // -----------------------------------------------------------------
        function toggleAuditDrawer() {
            const drawer = document.getElementById('audit-drawer');
            const isActive = drawer.classList.contains('active');
            if (!isActive) {
                drawer.classList.add('active');
                loadAuditLogs();
            } else {
                drawer.classList.remove('active');
            }
        }

        async function loadAuditLogs() {
            const list = document.getElementById('audit-logs-list');
            list.innerHTML = '<div class="empty-state">Loading audit logs...</div>';

            try {
                const logs = await apiCall('/api/v1/admin/audit?limit=50');
                if (logs.length === 0) {
                    list.innerHTML = '<div class="empty-state">No audit logs recorded yet.</div>';
                    return;
                }

                list.innerHTML = logs.map(l => {
                    const detailsStr = l.details ? JSON.stringify(l.details, null, 2) : '';
                    return '<div class="audit-item">' +
                        '<div class="audit-meta">' +
                            '<span class="audit-action">' + escapeHtml(l.action) + '</span>' +
                            '<span class="audit-time">' + formatDate(l.createdAt) + '</span>' +
                        '</div>' +
                        '<div style="color: var(--text-muted); font-size: 0.78rem;">' +
                            'Target: <span class="mono" style="font-weight: 600;">' + escapeHtml(l.entityType) + ' (' + escapeHtml(l.entityId) + ')</span> by <strong>' + escapeHtml(l.actor) + '</strong>' +
                        '</div>' +
                        (detailsStr ? '<pre class="audit-details mono">' + escapeHtml(detailsStr) + '</pre>' : '') +
                    '</div>';
                }).join('');
            } catch (err) {
                list.innerHTML = '<div class="empty-state" style="color: var(--status-archived);">' + escapeHtml(err.message) + '</div>';
            }
        }

        // -----------------------------------------------------------------
        // Initialization & CSP-Safe Event Binding
        // -----------------------------------------------------------------
        function bindEventListeners() {
            document.getElementById('btn-refresh-dashboard').addEventListener('click', loadDashboardData);
            document.getElementById('btn-toggle-audit').addEventListener('click', toggleAuditDrawer);
            document.getElementById('btn-close-audit').addEventListener('click', toggleAuditDrawer);
            document.getElementById('btn-logout').addEventListener('click', handleLogout);
            document.getElementById('btn-create-bot').addEventListener('click', openCreateBotModal);
            document.getElementById('btn-auth-submit').addEventListener('click', handleAuthSubmit);
            document.getElementById('btn-save-bot').addEventListener('click', saveBotForm);
            document.getElementById('btn-submit-version').addEventListener('click', submitNewVersion);
            document.getElementById('btn-add-credential').addEventListener('click', addCredentialSubmit);
            document.getElementById('search-input').addEventListener('input', handleSearch);
            document.getElementById('bot-form-name').addEventListener('input', handleNameSlugAutoFill);

            document.getElementById('btn-upload-icon').addEventListener('click', () => {
                document.getElementById('icon-file-input').click();
            });
            document.getElementById('icon-file-input').addEventListener('change', (e) => {
                handleIconFileUpload(e.target);
            });

            document.getElementById('pkg-dropzone').addEventListener('click', (e) => {
                if (e.target.id !== 'pkg-file-input') {
                    document.getElementById('pkg-file-input').click();
                }
            });
            document.getElementById('pkg-file-input').addEventListener('change', (e) => {
                handlePackageFileSelect(e.target);
            });

            document.querySelectorAll('.filter-tab').forEach(tab => {
                tab.addEventListener('click', () => {
                    setFilter(tab.getAttribute('data-filter'), tab);
                });
            });

            document.addEventListener('click', (e) => {
                const closeTarget = e.target.closest('[data-close-modal]');
                if (closeTarget) {
                    closeModal(closeTarget.getAttribute('data-close-modal'));
                    return;
                }

                const actionTarget = e.target.closest('[data-action]');
                if (!actionTarget) return;

                const action = actionTarget.getAttribute('data-action');
                const botId = actionTarget.getAttribute('data-bot-id');

                if (action === 'open-credentials') {
                    openCredentialModal(botId);
                } else if (action === 'open-versions') {
                    openVersionModal(botId);
                } else if (action === 'edit-bot') {
                    openEditBotModal(botId);
                } else if (action === 'delete-bot') {
                    confirmDeleteBot(botId, actionTarget.getAttribute('data-bot-name') || '');
                } else if (action === 'toggle-version-publish') {
                    const version = actionTarget.getAttribute('data-version');
                    const publish = actionTarget.getAttribute('data-publish') === 'true';
                    toggleVersionPublish(botId, version, publish);
                } else if (action === 'delete-credential') {
                    const credKey = actionTarget.getAttribute('data-cred-key');
                    deleteCredential(botId, credKey);
                }
            });

            document.addEventListener('change', (e) => {
                const selectTarget = e.target.closest('[data-action="change-lifecycle"]');
                if (!selectTarget) return;
                changeLifecycle(selectTarget.getAttribute('data-bot-id'), selectTarget.value);
            });
        }

        window.addEventListener('DOMContentLoaded', () => {
            bindEventListeners();
            if (checkAuthOrPrompt()) {
                loadDashboardData();
            }
        });
    </script>
</body>
</html>`;
}
