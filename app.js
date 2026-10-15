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
  favorites: [],
  dragPlaceholderIndex: null,
};

const FAVORITES_STORAGE_KEY = 'scanhelper-favoriten';
const FAVORITES_FILENAME = 'favoriten.json';

let DOKTYPEN = {};
let DOKUMENTKATEGORIEN = [];

const SONSTIGE_KEY = '__ohne_unterverzeichnis__';
const FACHSYSTEME = [
  { key: 'marke', label: 'Marke' },
  { key: 'patgbm', label: 'Patente / Gebrauchsmuster' },
  { key: 'designs', label: 'Design' },
  { key: 'zv', label: 'Zahlungsverkehr' },
];

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
  // segments[0] = Name des gewählten Wurzelverzeichnisses, segments[1] = direktes
  // Unterverzeichnis (eine Ebene), weitere Verschachtelung wird ignoriert.
  const segments = relPath.split('/');
  if (segments.length > 2) {
    const name = segments[1];
    return { key: name, label: name };
  }
  return { key: SONSTIGE_KEY, label: 'Sonstige' };
}

async function handleDirPicked(fileList) {
  const allFiles = Array.from(fileList);
  const pdfFiles = allFiles.filter((f) => f.name.toLowerCase().endsWith('.pdf'));
  state.sourceFiles = pdfFiles.map((file) => {
    const fullRelPath = file.webkitRelativePath || file.name;
    const group = detectGroup(fullRelPath);
    const relPath = fullRelPath.split('/').slice(1).join('/') || fullRelPath;
    return {
      id: 'f' + state.nextId++,
      file,
      relPath,
      group: group.key,
      groupLabel: group.label,
      name: file.name.replace(/\.pdf$/i, ''),
      pageCount: null,
      thumbDataUrl: null,
      pdfjsDoc: null,
    };
  });
  await renderQuellen();
  await loadFavoritesFromDirectory(allFiles);
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
  const labels = {};
  for (const sf of state.sourceFiles) {
    (groups[sf.group] ||= []).push(sf);
    labels[sf.group] = sf.groupLabel;
  }

  const keys = Object.keys(groups).sort((a, b) => {
    if (a === SONSTIGE_KEY) return 1;
    if (b === SONSTIGE_KEY) return -1;
    return labels[a].localeCompare(labels[b], 'de');
  });

  for (const key of keys) {
    const section = document.createElement('div');
    section.className = 'quellen-group';
    const collapsed = state.collapsedGroups.has(key);
    const h = document.createElement('div');
    h.className = 'quellen-group-title' + (collapsed ? ' collapsed' : '');
    h.innerHTML = `<span class="chevron">&#9660;</span> ${escapeHtml(labels[key])} (${groups[key].length})`;
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

async function createSeiteStapelItem(sf) {
  const doc = await loadPdfJsDoc(sf);
  const item = {
    id: 'it' + state.nextId++,
    type: 'seite-stapel',
    source: { kind: 'file', fileId: sf.id, pageCount: doc.numPages },
    relPath: sf.relPath,
    label: sf.name,
    thumbDataUrl: sf.thumbDataUrl || null,
  };
  if (!item.thumbDataUrl) {
    renderPageToDataUrl(doc, 1, 100)
      .then((dataUrl) => {
        item.thumbDataUrl = dataUrl;
        updateCardThumb(item);
      })
      .catch((err) => console.error(err));
  }
  return item;
}

async function addWholePdf(sf, atIndex) {
  const item = await createSeiteStapelItem(sf);
  insertItems([item], atIndex);
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

async function loadSourcePdfJsDocForLib(fileId) {
  const sf = state.sourceFiles.find((s) => s.id === fileId);
  return loadPdfJsDoc(sf);
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

function moveItemToIndex(id, toIndex) {
  const fromIdx = state.items.findIndex((it) => it.id === id);
  if (fromIdx === -1) return;
  const [moved] = state.items.splice(fromIdx, 1);
  let insertAt = fromIdx < toIndex ? toIndex - 1 : toIndex;
  insertAt = Math.max(0, Math.min(state.items.length, insertAt));
  state.items.splice(insertAt, 0, moved);
  renderStrip();
  renderTree();
}

// ---------- Favoriten ----------

function itemsToInstructions(items) {
  const instructions = [];
  let i = 0;
  while (i < items.length) {
    const item = items[i];
    if (item.type === 'paket-trenn') {
      instructions.push({ type: 'paket-trenn', kategorie: item.kategorie });
      i++;
    } else if (item.type === 'dokument-trenn') {
      instructions.push({ type: 'dokument-trenn', doktyp: item.doktyp });
      i++;
    } else if (item.type === 'zbh') {
      instructions.push({ type: 'zbh' });
      i++;
    } else if (item.type === 'seite-stapel') {
      instructions.push({ type: 'datei', relPath: item.relPath, label: item.label });
      i++;
    } else if (item.type === 'seite' && item.source && item.source.kind === 'generated') {
      const kind = item.source.dummy.kind;
      let count = 0;
      while (
        i < items.length &&
        items[i].type === 'seite' &&
        items[i].source &&
        items[i].source.kind === 'generated' &&
        items[i].source.dummy.kind === kind
      ) {
        count++;
        i++;
      }
      instructions.push({ type: 'seite-generiert', kind, anzahl: count });
    } else {
      i++;
    }
  }
  return instructions;
}

async function instructionsToItems(instructions) {
  const items = [];
  const missing = [];
  for (const instr of instructions) {
    if (instr.type === 'paket-trenn') {
      items.push(makePaketTrennItem(instr.kategorie));
    } else if (instr.type === 'dokument-trenn') {
      const fsKey = instr.doktyp ? instr.doktyp.fachsystem : null;
      items.push(makeDokumentTrennItem(fsKey, instr.doktyp ? instr.doktyp.nr : null));
    } else if (instr.type === 'zbh') {
      items.push(makeZbhItem());
    } else if (instr.type === 'seite-generiert') {
      items.push(...makeDummySeiten(instr.kind, instr.anzahl));
    } else if (instr.type === 'datei') {
      const sf = state.sourceFiles.find((s) => s.relPath === instr.relPath);
      if (sf) {
        items.push(await createSeiteStapelItem(sf));
      } else {
        missing.push(instr.relPath);
        items.push({ id: 'it' + state.nextId++, type: 'fehlend', relPath: instr.relPath, label: instr.label || instr.relPath });
      }
    }
  }
  return { items, missing };
}

function loadFavoritesFromStorage() {
  try {
    const raw = localStorage.getItem(FAVORITES_STORAGE_KEY);
    if (raw) state.favorites = JSON.parse(raw);
  } catch (err) {
    console.error('Favoriten aus localStorage konnten nicht geladen werden', err);
  }
}

function saveFavoritesToStorage() {
  try {
    localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify(state.favorites));
  } catch (err) {
    console.error('Favoriten konnten nicht in localStorage gespeichert werden', err);
  }
}

async function loadFavoritesFromDirectory(allFiles) {
  const favFile = allFiles.find((f) => {
    const rel = f.webkitRelativePath || f.name;
    const segments = rel.split('/');
    return segments.length === 2 && segments[1] === FAVORITES_FILENAME;
  });
  if (!favFile) return;
  try {
    const text = await favFile.text();
    const fromFile = JSON.parse(text);
    const existingNames = new Set(state.favorites.map((f) => f.name));
    for (const fav of fromFile) {
      if (!existingNames.has(fav.name)) {
        state.favorites.push(fav);
        existingNames.add(fav.name);
      }
    }
    renderFavoriten();
  } catch (err) {
    console.error('favoriten.json im Verzeichnis konnte nicht gelesen werden', err);
  }
}

async function applyFavorite(fav) {
  const { items, missing } = await instructionsToItems(fav.items);
  insertItems(items);
  if (missing.length) {
    alert(`Favorit "${fav.name}" angewendet, aber ${missing.length} Datei(en) fehlen im gewählten Verzeichnis:\n` + missing.join('\n'));
  }
}

function saveCurrentAsFavorite(name) {
  const instructions = itemsToInstructions(state.items);
  state.favorites = state.favorites.filter((f) => f.name !== name);
  state.favorites.push({ name, items: instructions });
  saveFavoritesToStorage();
  renderFavoriten();
}

function deleteFavorite(name) {
  state.favorites = state.favorites.filter((f) => f.name !== name);
  saveFavoritesToStorage();
  renderFavoriten();
}

function exportFavoritesFile() {
  const blob = new Blob([JSON.stringify(state.favorites, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = FAVORITES_FILENAME;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}

function renderFavoriten() {
  const list = document.getElementById('favoritenList');
  if (!list) return;
  list.innerHTML = '';
  if (!state.favorites.length) {
    list.innerHTML = '<div class="hint">Noch keine Favoriten.</div>';
    return;
  }
  for (const fav of state.favorites) {
    const row = document.createElement('div');
    row.className = 'favorit-row';
    const info = document.createElement('div');
    info.className = 'favorit-info';
    info.innerHTML = `<div class="favorit-name">${escapeHtml(fav.name)}</div><div class="favorit-meta">${fav.items.length} Elemente</div>`;
    row.appendChild(info);
    const applyBtn = document.createElement('button');
    applyBtn.textContent = 'Anwenden';
    applyBtn.addEventListener('click', () => applyFavorite(fav));
    row.appendChild(applyBtn);
    const delBtn = document.createElement('div');
    delBtn.className = 'del-btn favorit-del';
    delBtn.textContent = '×';
    delBtn.title = 'Favorit löschen';
    delBtn.addEventListener('click', () => deleteFavorite(fav.name));
    row.appendChild(delBtn);
    list.appendChild(row);
  }
}

// ---------- Streifen (Strip) ----------

function makeDropPlaceholder() {
  const el = document.createElement('div');
  el.className = 'drop-placeholder';
  return el;
}

function getDropIndexFromPosition(clientX, clientY) {
  const cardEls = [...document.querySelectorAll('#strip .item-card[data-id]')];
  if (!cardEls.length) return 0;
  const withRects = cardEls.map((el) => ({ el, rect: el.getBoundingClientRect() }));
  const sameRow = withRects.filter((c) => clientY >= c.rect.top && clientY <= c.rect.bottom);
  const candidates = sameRow.length ? sameRow : withRects;

  let closest = null;
  let closestDist = Infinity;
  for (const c of candidates) {
    const cx = c.rect.left + c.rect.width / 2;
    const dist = Math.abs(clientX - cx);
    if (dist < closestDist) {
      closestDist = dist;
      closest = c;
    }
  }
  const idx = state.items.findIndex((it) => it.id === closest.el.dataset.id);
  const before = clientX < closest.rect.left + closest.rect.width / 2;
  return before ? idx : idx + 1;
}

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
  } else if (item.type === 'fehlend') {
    card.classList.add('sheet-fehlend');
    card.textContent = 'Fehlt:\n' + (item.label || item.relPath);
    card.title = `Datei nicht im gewählten Verzeichnis gefunden: ${item.relPath}`;
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
    if (state.dragPlaceholderIndex === i) appendTarget.appendChild(makeDropPlaceholder());
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

  if (state.dragPlaceholderIndex === state.items.length) appendTarget.appendChild(makeDropPlaceholder());

  const endZone = document.createElement('div');
  endZone.className = 'insert-end';
  if (state.insertIndex === null) endZone.classList.add('active');
  endZone.addEventListener('click', () => {
    state.insertIndex = null;
    renderStrip();
  });
  strip.appendChild(endZone);

  document.getElementById('stripEmpty').style.display = state.items.length ? 'none' : 'block';
  const totalPages = state.items.reduce((sum, it) => {
    if (it.type === 'seite-stapel') return sum + it.source.pageCount;
    if (it.type === 'fehlend') return sum;
    return sum + 1;
  }, 0);
  const missingCount = state.items.filter((it) => it.type === 'fehlend').length;
  setStatus(totalPages ? `${totalPages} Seite(n)` + (missingCount ? `, ${missingCount} fehlend` : '') : '');
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
    } else if (item.type === 'fehlend') {
      ensureLeadingPaket();
      ensureLeadingDok();
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
  const missing = state.items.filter((it) => it.type === 'fehlend');
  if (missing.length) {
    const ok = confirm(
      `${missing.length} Datei(en) fehlen im gewählten Verzeichnis und werden im PDF übersprungen:\n` +
        missing.map((m) => m.relPath).join('\n') +
        '\n\nTrotzdem erzeugen?'
    );
    if (!ok) return null;
  }
  const scanMode = document.getElementById('scanModeToggle').checked;
  setStatus('Erzeuge PDF…');
  try {
    const bytes = await PdfGen.buildFinalPdf(state.items, loadSourceDocForLib, {
      scanMode,
      loadSourcePdfJsDoc: loadSourcePdfJsDocForLib,
      onProgress: (done, total) => setStatus(`Erzeuge PDF… (${done}/${total})`),
    });
    renderStrip();
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

function makePaketTrennItem(kategorie) {
  return { id: 'it' + state.nextId++, type: 'paket-trenn', kategorie: kategorie || null };
}

function makeDokumentTrennItem(fsKey, nr) {
  let doktyp = null;
  if (nr) {
    const entry = (DOKTYPEN[fsKey] || []).find((d) => String(d.nr) === String(nr));
    if (entry) doktyp = { nr: entry.nr, name: entry.name, fachsystem: fsKey };
  }
  return { id: 'it' + state.nextId++, type: 'dokument-trenn', doktyp };
}

function makeZbhItem() {
  return { id: 'it' + state.nextId++, type: 'zbh' };
}

function makeDummySeiten(kind, anzahl) {
  const n = Math.max(1, Math.min(50, anzahl || 1));
  const newItems = [];
  for (let i = 1; i <= n; i++) {
    newItems.push({
      id: 'it' + state.nextId++,
      type: 'seite',
      source: {
        kind: 'generated',
        dummy: { kind, pageLabel: kind === 'stempel' ? `Seite ${i} von ${n}` : `Dummy-Seite ${i}/${n}` },
      },
      label: `Dummy ${i}/${n}`,
    });
  }
  return newItems;
}

document.querySelector('[data-gen="paket"]').addEventListener('click', () => {
  insertItems([makePaketTrennItem(document.getElementById('genPaketKategorie').value)]);
});

document.querySelector('[data-gen="dokument"]').addEventListener('click', () => {
  const fsKey = document.getElementById('genDokFachsystem').value;
  const nr = document.getElementById('genDokTyp').value;
  insertItems([makeDokumentTrennItem(fsKey, nr)]);
});

document.querySelector('[data-gen="zbh"]').addEventListener('click', () => {
  insertItems([makeZbhItem()]);
});

document.querySelector('[data-gen="seite"]').addEventListener('click', () => {
  const anzahl = parseInt(document.getElementById('genSeiteAnzahl').value, 10);
  const kind = document.getElementById('genSeiteInhalt').value;
  insertItems(makeDummySeiten(kind, anzahl));
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

// ---------- Streifen: Drag & Drop mit wanderndem Platzhalter ----------

const stripEl = document.getElementById('strip');

stripEl.addEventListener('dragover', (e) => {
  e.preventDefault();
  const idx = getDropIndexFromPosition(e.clientX, e.clientY);
  if (idx !== state.dragPlaceholderIndex) {
    state.dragPlaceholderIndex = idx;
    renderStrip();
  }
});

stripEl.addEventListener('dragleave', (e) => {
  if (!stripEl.contains(e.relatedTarget)) {
    state.dragPlaceholderIndex = null;
    renderStrip();
  }
});

stripEl.addEventListener('drop', (e) => {
  e.preventDefault();
  const idx = state.dragPlaceholderIndex !== null ? state.dragPlaceholderIndex : getDropIndexFromPosition(e.clientX, e.clientY);
  state.dragPlaceholderIndex = null;
  const itemId = e.dataTransfer.getData('text/x-item-id');
  const sourceFileId = e.dataTransfer.getData('text/x-source-file');
  if (itemId) {
    moveItemToIndex(itemId, idx);
  } else if (sourceFileId) {
    const sf = state.sourceFiles.find((s) => s.id === sourceFileId);
    if (sf) addWholePdf(sf, idx);
    else renderStrip();
  } else {
    renderStrip();
  }
});

document.addEventListener('dragend', () => {
  if (state.dragPlaceholderIndex !== null) {
    state.dragPlaceholderIndex = null;
    renderStrip();
  }
});

// ---------- Verzeichnis-Picker ----------

document.getElementById('btnPickDir').addEventListener('click', () => {
  document.getElementById('dirInput').click();
});
document.getElementById('dirInput').addEventListener('change', (e) => {
  handleDirPicked(e.target.files);
});

// ---------- Hilfe ----------

const helpOverlay = document.getElementById('helpOverlay');
document.getElementById('btnHelp').addEventListener('click', () => helpOverlay.classList.add('open'));
document.getElementById('btnHelpClose').addEventListener('click', () => helpOverlay.classList.remove('open'));
helpOverlay.addEventListener('click', (e) => {
  if (e.target === helpOverlay) helpOverlay.classList.remove('open');
});
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') helpOverlay.classList.remove('open');
});

// ---------- Favoriten-Buttons ----------

document.getElementById('btnFavSave').addEventListener('click', () => {
  const nameInput = document.getElementById('favName');
  const name = nameInput.value.trim();
  if (!name) {
    alert('Bitte einen Namen für den Favoriten eingeben.');
    return;
  }
  if (!state.items.length) {
    alert('Noch keine Seiten im Dokument.');
    return;
  }
  saveCurrentAsFavorite(name);
  nameInput.value = '';
});

document.getElementById('btnFavExport').addEventListener('click', () => {
  if (!state.favorites.length) {
    alert('Noch keine Favoriten vorhanden.');
    return;
  }
  exportFavoritesFile();
});

// ---------- Init ----------

(async function init() {
  loadData();
  populateGeneratorForms();
  loadFavoritesFromStorage();
  renderFavoriten();
  renderStrip();
  renderTree();
})();
