export const prepareFileViewer = () => {
  const viewer = window.open('about:blank', '_blank');
  if (!viewer) return null;
  viewer.opener = null;
  viewer.document.title = 'Opening document…';
  viewer.document.body.innerHTML = '<p style="font:16px system-ui;padding:24px;color:#334155">Opening document…</p>';
  return viewer;
};

export const showBlobInViewer = (response, viewer) => {
  const contentType = response.headers?.['content-type'] || response.data?.type || 'application/octet-stream';
  const blob = response.data instanceof Blob && response.data.type === contentType
    ? response.data
    : new Blob([response.data], { type: contentType });
  const url = URL.createObjectURL(blob);

  if (viewer && !viewer.closed) {
    viewer.location.replace(url);
  } else {
    const link = document.createElement('a');
    link.href = url;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  window.setTimeout(() => URL.revokeObjectURL(url), 300_000);
};
