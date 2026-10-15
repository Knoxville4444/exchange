const pdfjsLib = window.pdfjsLib;
const PdfGen = window.PdfGen;

pdfjsLib.GlobalWorkerOptions.workerSrc = 'lib/pdf.worker.min.js';

const state = {
  items: [],
  insertIndex: null,
  sourceFiles: [],
  sourceDocCache: new Map(),
  nextId: 1,
  collapsedGroups: new Set(),
};

let DOKTYPEN = {};
let DOKUMENTKATEGORIEN = [];

const SCHUTZRECHT_MAP = {
  marken: { key: 'marke', label: 'Marke' },
  patente: { key: 'patgbm', label: 'Patente / Gebrauchsmuster' },
  gebrauchsmuster: { key: 'patgbm', label: 'Patente / Gebrauchsmuster' },
  designs: { key: 'designs', label: 'Design' },
  zahlungsverkehr: { key: 'zv', label: 'Zahlungsverkehr' },
};
const FACHSYSTEME = [
  { key: 'marke', label: 'Marke' },
  { key: 'patgbm', label: 'Patente / Gebrauchsmuster' },
  { key: 'designs', label: 'Design' },
  { key: 'zv', label: 'Zahlungsverkehr' },
];
const GROUP_ORDER = ['marke', 'patgbm', 'designs', 'zv', 'sonstige'];
const GROUP_LABELS = { marke: 'Marke', patgbm: 'Patente / Gebrauchsmuster', designs: 'Design', zv: 'Zahlungsverkehr', sonstige: 'Sonstige' };

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function setStatus(text) {
  document.getElementById('status').textContent = text;
}

function loadData() {
  DOKTYPEN = window.DOKTYPEN;
  DOKUMENTKATEGORIEN = window.DOKUMENTKATEGORIEN;
}

function populateGeneratorForms() {
  const katSelect = document.getElementById('genPaketKategorie');
  katSelect.innerHTML =
    '<option value="">— keine Kategorie —</option>' +
    DOKUMENTKATEGORIEN.map((k) => `<option value="${k}">${escapeHtml(PdfGen.humanize(k))}</option>`).join('');

  const fsSelect = document.getElementById('genDokFachsystem');
  fsSelect.innerHTML = FACHSYSTEME.map((f) => `<option value="${f.key}">${f.label}</option>`).join('');
  fsSelect.addEventListener('change', updateDoktypOptions);
  updateDoktypOptions();
}

function updateDoktypOptions() {
  const fsKey = document.getElementById('genDokFachsystem').value;
  const list = DOKTYPEN[fsKey] || [];
  const sel = document.getElementById('genDokTyp');
  sel.innerHTML =
    '<option value="">— kein Dokumenttyp —</option>' +
    list.map((d) => `<option value="${d.nr}">${d.nr} - ${escapeHtml(d.name)}</option>`).join('');
}

// ---------- Quellen ----------

function detectGroup(relPath) {
  const segments = relPath.split('/');
  for (const seg of segments) {
    const hit = SCHUTZRECHT_MAP[seg.toLowerCase()];
    if (hit) return hit;
  }
  return { key: 'sonstige', label: 'Sonstige' };
}

async function handleDirPicked(fileList) {
  const files = Array.from(fileList).filter((f) => f.name.toLowerCase().endsWith('.pdf'));
  state.sourceFiles = files.map((file) => {
    const relPath = file.webkitRelativePath || file.name;
    const group = detectGroup(relPath);
    return {
      id: 'f' + state.nextId++,
      file,
      group: group.key,
      groupLabel: group.label,
      name: file.name.replace(/\.pdf$/i, ''),
      pageCount: null,
      thumbDataUrl: null,
      pdfjsDoc: null,
    };
  });
  await renderQuellen();
}

async function loadPdfJsDoc(sf) {
  if (sf.pdfjsDoc) return sf.pdfjsDoc;
  const buf = await sf.file.arrayBuffer();
  const doc = await pdfjsLib.getDocument({ data: buf }).promise;
  sf.pdfjsDoc = doc;
  sf.pageCount = doc.numPages;
  return doc;
}

async function renderPageToDataUrl(doc, pageNumber, targetWidth) {
  const page = await doc.getPage(pageNumber);
  const viewport = page.getViewport({ scale: 1 });
  const scale = targetWidth / viewport.width;
  const scaledViewport = page.getViewport({ scale });
  const canvas = document.createElement('canvas');
  canvas.width = scaledViewport.width;
  canvas.height = scaledViewport.height;
  const ctx = canvas.getContext('2d');
  await page.render({ canvasContext: ctx, viewport: scaledViewport }).promise;
  return canvas.toDataURL('image/png');
}

async function renderQuellen() {
  const container = document.getElementById('quellenGroups');
  container.innerHTML = '';
  const groups = {};
  for (const sf of state.sourceFiles) (groups[sf.group] ||= []).push(sf);

  for (const key of GROUP_ORDER) {
    if (!groups[key] || !groups[key].length) continue;
    const section = document.createElement('div');
    section.className = 'quellen-group';
    const collapsed = state.collapsedGroups.has(key);
    const h = document.createElement('div');
    h.className = 'quellen-group-title' + (collapsed ? ' collapsed' : '');
    h.innerHTML = `<span class="chevron">&#9660;</span> ${GROUP_LABELS[key]} (${groups[key].length})`;
    section.appendChild(h);
    const grid = document.createElement('div');
    grid.className = 'quellen-grid' + (collapsed ? ' collapsed' : '');
    section.appendChild(grid);
    container.appendChild(section);

    h.addEventListener('click', () => {
      if (state.collapsedGroups.has(key)) state.collapsedGroups.delete(key);
      else state.collapsedGroups.add(key);
      h.classList.toggle('collapsed');
      grid.classList.toggle('collapsed');
    });

    for (const sf of groups[key]) {
      const card = document.createElement('div');
      card.className = 'source-card';
      card.draggable = true;
      card.title = sf.name;
      card.innerHTML = `<div class="thumb-box"></div><div class="source-name">${escapeHtml(sf.name)}</div>`;
      grid.appendChild(card);
      card.addEventListener('click', () => addWholePdf(sf));
      card.addEventListener('dragstart', (e) => {
        e.dataTransfer.setData('text/x-source-file', sf.id);
      });

      loadPdfJsDoc(sf)
        .then(() => renderPageToDataUrl(sf.pdfjsDoc, 1, 105))
        .then((dataUrl) => {
          sf.thumbDataUrl = dataUrl;
          card.querySelector('.thumb-box').style.backgroundImage = `url(${dataUrl})`;
          if (sf.pageCount) {
            const badge = document.createElement('div');
            badge.className = 'page-badge';
            badge.textContent = sf.pageCount;
            card.appendChild(badge);
          }
        })
        .catch((err) => console.error('Thumbnail-Fehler', sf.name, err));
    }
  }
}

async function addWholePdf(sf, atIndex) {
  const doc = await loadPdfJsDoc(sf);
  const item = {
    id: 'it' + state.nextId++,
    type: 'seite-stapel',
    source: { kind: 'file', fileId: sf.id, pageCount: doc.numPages },
    label: sf.name,
    thumbDataUrl: sf.thumbDataUrl || null,
  };
  insertItems([item], atIndex);
  if (!item.thumbDataUrl) {
    renderPageToDataUrl(doc, 1, 100)
      .then((dataUrl) => {
        item.thumbDataUrl = dataUrl;
        updateCardThumb(item);
      })
      .catch((err) => console.error(err));
  }
}

function updateCardThumb(item) {
  const el = document.querySelector(`.item-card[data-id="${item.id}"]`);
  if (el && item.thumbDataUrl) el.style.backgroundImage = `url(${item.thumbDataUrl})`;
}

async function loadSourceDocForLib(fileId) {
  if (state.sourceDocCache.has(fileId)) return state.sourceDocCache.get(fileId);
  const sf = state.sourceFiles.find((s) => s.id === fileId);
  const bytes = await sf.file.arrayBuffer();
  const doc = await window.PDFLib.PDFDocument.load(bytes);
  state.sourceDocCache.set(fileId, doc);
  return doc;
}

// ---------- Item-Verwaltung ----------

function insertItems(newItems, atIndex) {
  const idx = atIndex === undefined ? state.insertIndex : atIndex;
  if (idx === null || idx === undefined || idx >= state.items.length) {
    state.items.push(...newItems);
    state.insertIndex = null;
  } else {
    state.items.splice(idx, 0, ...newItems);
    state.insertIndex = idx + newItems.length;
  }
  renderStrip();
  renderTree();
}

function removeItem(id) {
  state.items = state.items.filter((it) => it.id !== id);
  renderStrip();
  renderTree();
}

function removeGroup(startIndex, level) {
  let end = startIndex + 1;
  while (end < state.items.length) {
    const t = state.items[end].type;
    const isBoundary = level === 'paket' ? t === 'paket-trenn' : t === 'paket-trenn' || t === 'dokument-trenn' || t === 'zbh';
    if (isBoundary) break;
    end++;
  }
  state.items.splice(startIndex, end - startIndex);
  if (state.insertIndex !== null) {
    if (state.insertIndex > startIndex && state.insertIndex <= end) state.insertIndex = startIndex;
    else if (state.insertIndex > end) state.insertIndex -= end - startIndex;
  }
  renderStrip();
  renderTree();
}

function makeGroupDelBtn(startIndex, level) {
  const btn = document.createElement('div');
  btn.className = 'group-del-btn';
  btn.textContent = '×';
  btn.title = level === 'paket' ? 'Ganzes Paket löschen' : 'Ganzes Dokument löschen';
  btn.addEventListener('click', (e) => {
    e.stopPropagation();
    removeGroup(startIndex, level);
  });
  return btn;
}

function moveItem(id, beforeId) {
  const fromIdx = state.items.findIndex((it) => it.id === id);
  if (fromIdx === -1) return;
  const [moved] = state.items.splice(fromIdx, 1);
  if (beforeId === null) {
    state.items.push(moved);
  } else {
    const toIdx = state.items.findIndex((it) => it.id === beforeId);
    state.items.splice(toIdx === -1 ? state.items.length : toIdx, 0, moved);
  }
  renderStrip();
  renderTree();
}

// ---------- Streifen (Strip) ----------

function makeCard(item) {
  const card = document.createElement('div');
  card.className = 'item-card';
  card.dataset.id = item.id;
  card.draggable = true;

  if (item.type === 'paket-trenn') {
    card.classList.add('sheet-paket');
    card.textContent = 'Paket' + (item.kategorie ? '\n' + PdfGen.humanize(item.kategorie) : '');
  } else if (item.type === 'dokument-trenn') {
    card.classList.add('sheet-dokument');
    card.textContent = item.doktyp ? `D${String(item.doktyp.nr).padStart(3, '0')}` : 'Dokument';
  } else if (item.type === 'zbh') {
    card.classList.add('sheet-zbh');
    card.textContent = 'ZBH';
  } else if (item.type === 'seite') {
    if (item.thumbDataUrl) card.style.backgroundImage = `url(${item.thumbDataUrl})`;
    else card.textContent = item.label || '…';
  } else if (item.type === 'seite-stapel') {
    card.classList.add('stapel');
    if (item.thumbDataUrl) card.style.backgroundImage = `url(${item.thumbDataUrl})`;
    else card.textContent = item.label || '…';
    if (item.source.pageCount > 1) {
      const badge = document.createElement('div');
      badge.className = 'page-badge';
      badge.textContent = item.source.pageCount;
      card.appendChild(badge);
    }
  }

  const del = document.createElement('div');
  del.className = 'del-btn';
  del.textContent = '×';
  del.addEventListener('click', (e) => {
    e.stopPropagation();
    removeItem(item.id);
  });
  card.appendChild(del);

  card.addEventListener('click', () => {
    state.insertIndex = state.items.findIndex((it) => it.id === item.id) + 1;
    renderStrip();
  });

  card.addEventListener('dragstart', (e) => {
    e.dataTransfer.setData('text/x-item-id', item.id);
  });
  card.addEventListener('dragover', (e) => {
    e.preventDefault();
    card.classList.add('drag-over');
  });
  card.addEventListener('dragleave', () => card.classList.remove('drag-over'));
  card.addEventListener('drop', (e) => {
    e.preventDefault();
    card.classList.remove('drag-over');
    const itemId = e.dataTransfer.getData('text/x-item-id');
    const sourceFileId = e.dataTransfer.getData('text/x-source-file');
    if (itemId) {
      moveItem(itemId, item.id);
    } else if (sourceFileId) {
      const sf = state.sourceFiles.find((s) => s.id === sourceFileId);
      const idx = state.items.findIndex((it) => it.id === item.id);
      if (sf) addWholePdf(sf, idx);
    }
  });

  if (state.insertIndex === state.items.findIndex((it) => it.id === item.id)) {
    card.classList.add('selected');
  }

  if (item.type === 'seite-stapel') {
    const wrap = document.createElement('div');
    wrap.className = 'stapel-wrap';
    wrap.appendChild(card);
    const caption = document.createElement('div');
    caption.className = 'stapel-caption';
    caption.textContent = item.label || '';
    caption.title = item.label || '';
    wrap.appendChild(caption);
    return wrap;
  }

  return card;
}

function renderStrip() {
  const strip = document.getElementById('strip');
  strip.innerHTML = '';

  let appendTarget = strip;
  let paketInner = null;
  let paketOpen = false;
  let dokOpen = false;

  function openImplicitPaket(startIdx) {
    const paketDiv = document.createElement('div');
    paketDiv.className = 'group-paket';
    const label = document.createElement('div');
    label.className = 'group-label';
    label.textContent = 'Paket (unbenannt)';
    paketDiv.appendChild(label);
    paketDiv.appendChild(makeGroupDelBtn(startIdx, 'paket'));
    const inner = document.createElement('div');
    inner.className = 'group-inner';
    paketDiv.appendChild(inner);
    strip.appendChild(paketDiv);
    paketInner = inner;
    appendTarget = inner;
    paketOpen = true;
  }

  function openImplicitDok(startIdx) {
    const dokDiv = document.createElement('div');
    dokDiv.className = 'group-dokument';
    const label = document.createElement('div');
    label.className = 'group-label';
    label.textContent = 'Dokument (unbenannt)';
    dokDiv.appendChild(label);
    dokDiv.appendChild(makeGroupDelBtn(startIdx, 'dokument'));
    const inner = document.createElement('div');
    inner.className = 'group-inner';
    dokDiv.appendChild(inner);
    (paketInner || strip).appendChild(dokDiv);
    appendTarget = inner;
    dokOpen = true;
  }

  for (let i = 0; i < state.items.length; i++) {
    const item = state.items[i];
    if (item.type === 'paket-trenn') {
      const paketDiv = document.createElement('div');
      paketDiv.className = 'group-paket';
      const label = document.createElement('div');
      label.className = 'group-label';
      label.textContent = 'Paket' + (item.kategorie ? ' · ' + PdfGen.humanize(item.kategorie) : '');
      paketDiv.appendChild(label);
      paketDiv.appendChild(makeGroupDelBtn(i, 'paket'));
      const inner = document.createElement('div');
      inner.className = 'group-inner';
      paketDiv.appendChild(inner);
      strip.appendChild(paketDiv);
      paketInner = inner;
      appendTarget = inner;
      paketOpen = true;
      dokOpen = false;
      appendTarget.appendChild(makeCard(item));
    } else if (item.type === 'dokument-trenn' || item.type === 'zbh') {
      if (!paketOpen) openImplicitPaket(i);
      const dokDiv = document.createElement('div');
      dokDiv.className = item.type === 'zbh' ? 'group-dokument group-zbh' : 'group-dokument';
      const label = document.createElement('div');
      label.className = 'group-label';
      label.textContent =
        item.type === 'zbh' ? 'ZBH' : 'Dokument' + (item.doktyp ? ' · D' + String(item.doktyp.nr).padStart(3, '0') : '');
      dokDiv.appendChild(label);
      dokDiv.appendChild(makeGroupDelBtn(i, 'dokument'));
      const inner = document.createElement('div');
      inner.className = 'group-inner';
      dokDiv.appendChild(inner);
      (paketInner || strip).appendChild(dokDiv);
      appendTarget = inner;
      dokOpen = true;
      appendTarget.appendChild(makeCard(item));
    } else {
      if (!paketOpen) openImplicitPaket(i);
      if (!dokOpen) openImplicitDok(i);
      appendTarget.appendChild(makeCard(item));
    }
  }

  const endZone = document.createElement('div');
  endZone.className = 'insert-end';
  if (state.insertIndex === null) endZone.classList.add('active');
  endZone.addEventListener('click', () => {
    state.insertIndex = null;
    renderStrip();
  });
  endZone.addEventListener('dragover', (e) => e.preventDefault());
  endZone.addEventListener('drop', (e) => {
    e.preventDefault();
    const itemId = e.dataTransfer.getData('text/x-item-id');
    const sourceFileId = e.dataTransfer.getData('text/x-source-file');
    if (itemId) moveItem(itemId, null);
    else if (sourceFileId) {
      const sf = state.sourceFiles.find((s) => s.id === sourceFileId);
      if (sf) addWholePdf(sf, null);
    }
  });
  strip.appendChild(endZone);

  document.getElementById('stripEmpty').style.display = state.items.length ? 'none' : 'block';
  const totalPages = state.items.reduce((sum, it) => sum + (it.type === 'seite-stapel' ? it.source.pageCount : 1), 0);
  setStatus(totalPages ? `${totalPages} Seite(n)` : '');
}

// ---------- Gliederung (Baum) ----------

function renderTree() {
  const el = document.getElementById('treeContent');
  el.innerHTML = '';
  if (!state.items.length) {
    el.innerHTML = '<div class="tree-empty">Noch leer</div>';
    return;
  }

  let paketNode = null;
  let dokNode = null;
  let dokCount = 0;
  let implicitPaketCreated = false;
  let implicitDokCreated = false;

  function ensureLeadingPaket() {
    if (paketNode === null && !implicitPaketCreated) {
      const n = document.createElement('div');
      n.className = 'tree-node tree-paket';
      n.textContent = 'Paket (unbenannt)';
      el.appendChild(n);
      paketNode = n;
      implicitPaketCreated = true;
    }
  }

  function ensureLeadingDok() {
    if (dokNode === null && !implicitDokCreated) {
      const n = document.createElement('div');
      n.className = 'tree-node tree-dok';
      n.textContent = 'Dokument (unbenannt)';
      el.appendChild(n);
      dokNode = n;
      implicitDokCreated = true;
    }
  }

  function flushDok() {
    if (dokNode && dokCount > 0) {
      const c = document.createElement('div');
      c.className = 'tree-seiten-count';
      c.textContent = `${dokCount} Seite(n)`;
      dokNode.after(c);
    }
    dokNode = null;
    dokCount = 0;
    implicitDokCreated = false;
  }

  for (const item of state.items) {
    const idx = state.items.indexOf(item);
    if (item.type === 'paket-trenn') {
      flushDok();
      const n = document.createElement('div');
      n.className = 'tree-node tree-paket';
      n.textContent = 'Paket' + (item.kategorie ? ' · ' + PdfGen.humanize(item.kategorie) : '');
      n.addEventListener('click', () => scrollToItem(item.id));
      el.appendChild(n);
      paketNode = n;
    } else if (item.type === 'dokument-trenn' || item.type === 'zbh') {
      ensureLeadingPaket();
      flushDok();
      const n = document.createElement('div');
      n.className = 'tree-node tree-dok';
      n.textContent = item.type === 'zbh' ? 'ZBH' : 'Dokument' + (item.doktyp ? ' · D' + String(item.doktyp.nr).padStart(3, '0') : '');
      n.addEventListener('click', () => scrollToItem(item.id));
      el.appendChild(n);
      dokNode = n;
    } else if (item.type === 'seite') {
      ensureLeadingPaket();
      ensureLeadingDok();
      dokCount++;
    } else if (item.type === 'seite-stapel') {
      ensureLeadingPaket();
      ensureLeadingDok();
      dokCount += item.source.pageCount;
    }
  }
  flushDok();
}

function scrollToItem(id) {
  const el = document.querySelector(`.item-card[data-id="${id}"]`);
  if (el) el.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
}

// ---------- Export ----------

async function exportPdf() {
  if (!state.items.length) {
    alert('Noch keine Seiten im Dokument.');
    return null;
  }
  setStatus('Erzeuge PDF…');
  try {
    const bytes = await PdfGen.buildFinalPdf(state.items, loadSourceDocForLib);
    setStatus(`${state.items.length} Seite(n)`);
    return new Blob([bytes], { type: 'application/pdf' });
  } catch (err) {
    console.error(err);
    setStatus('Fehler beim Erzeugen');
    alert('Fehler beim Erzeugen des PDFs: ' + err.message);
    return null;
  }
}

document.getElementById('btnDownload').addEventListener('click', async () => {
  const blob = await exportPdf();
  if (!blob) return;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'testdokument.pdf';
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
});

document.getElementById('btnPrint').addEventListener('click', async () => {
  const blob = await exportPdf();
  if (!blob) return;
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank');
});

// ---------- Generator-Buttons ----------

document.querySelector('[data-gen="paket"]').addEventListener('click', () => {
  const kategorie = document.getElementById('genPaketKategorie').value || null;
  insertItems([{ id: 'it' + state.nextId++, type: 'paket-trenn', kategorie }]);
});

document.querySelector('[data-gen="dokument"]').addEventListener('click', () => {
  const nr = document.getElementById('genDokTyp').value;
  const fsKey = document.getElementById('genDokFachsystem').value;
  let doktyp = null;
  if (nr) {
    const entry = (DOKTYPEN[fsKey] || []).find((d) => String(d.nr) === nr);
    if (entry) doktyp = { nr: entry.nr, name: entry.name };
  }
  insertItems([{ id: 'it' + state.nextId++, type: 'dokument-trenn', doktyp }]);
});

document.querySelector('[data-gen="zbh"]').addEventListener('click', () => {
  insertItems([{ id: 'it' + state.nextId++, type: 'zbh' }]);
});

document.querySelector('[data-gen="seite"]').addEventListener('click', () => {
  const anzahl = Math.max(1, Math.min(50, parseInt(document.getElementById('genSeiteAnzahl').value, 10) || 1));
  const kind = document.getElementById('genSeiteInhalt').value;
  const newItems = [];
  for (let i = 1; i <= anzahl; i++) {
    newItems.push({
      id: 'it' + state.nextId++,
      type: 'seite',
      source: {
        kind: 'generated',
        dummy: { kind, pageLabel: kind === 'stempel' ? `Seite ${i} von ${anzahl}` : `Dummy-Seite ${i}/${anzahl}` },
      },
      label: `Dummy ${i}/${anzahl}`,
    });
  }
  insertItems(newItems);
});

// ---------- Tabs ----------

for (const tab of document.querySelectorAll('.tab')) {
  tab.addEventListener('click', () => {
    for (const t of document.querySelectorAll('.tab')) t.classList.remove('active');
    for (const c of document.querySelectorAll('.tab-content')) c.classList.remove('active');
    tab.classList.add('active');
    document.getElementById('tab' + tab.dataset.tab.charAt(0).toUpperCase() + tab.dataset.tab.slice(1)).classList.add('active');
  });
}

// ---------- Verzeichnis-Picker ----------

document.getElementById('btnPickDir').addEventListener('click', () => {
  document.getElementById('dirInput').click();
});
document.getElementById('dirInput').addEventListener('change', (e) => {
  handleDirPicked(e.target.files);
});

// ---------- Init ----------

(async function init() {
  loadData();
  populateGeneratorForms();
  renderStrip();
  renderTree();
})();
