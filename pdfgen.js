window.PdfGen = (function () {
const { PDFDocument, rgb, StandardFonts } = window.PDFLib;

const PAGE_W = 595.28;
const PAGE_H = 841.89;
const MARGIN = 40;

function humanize(code) {
  return code
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

function drawQr(page, text, x, y, size, black) {
  const qr = qrcode(0, 'M');
  qr.addData(text);
  qr.make();
  const count = qr.getModuleCount();
  const cell = size / count;
  for (let row = 0; row < count; row++) {
    for (let col = 0; col < count; col++) {
      if (qr.isDark(row, col)) {
        page.drawRectangle({
          x: x + col * cell,
          y: y + size - (row + 1) * cell,
          width: cell + 0.3,
          height: cell + 0.3,
          color: black,
        });
      }
    }
  }
}

function drawPatchFrame(page, black) {
  const barHeights = [10, 4, 4, 10];
  let y = PAGE_H - MARGIN;
  for (const h of barHeights) {
    page.drawRectangle({ x: MARGIN, y: y - h, width: PAGE_W - 2 * MARGIN, height: h, color: black });
    y -= h + 4;
  }
  y = MARGIN;
  for (const h of barHeights.slice().reverse()) {
    page.drawRectangle({ x: MARGIN, y, width: PAGE_W - 2 * MARGIN, height: h, color: black });
    y += h + 4;
  }
  const vBarW = [8, 4, 4];
  const barH = 110;
  const topY = PAGE_H - MARGIN - 50 - barH;
  let vx = MARGIN;
  for (const w of vBarW) {
    page.drawRectangle({ x: vx, y: topY, width: w, height: barH, color: black });
    vx += w + 5;
  }
  let vxr = PAGE_W - MARGIN;
  for (const w of vBarW.slice().reverse()) {
    vxr -= w;
    page.drawRectangle({ x: vxr, y: topY, width: w, height: barH, color: black });
    vxr -= 5;
  }
}

async function makeFonts(pdfDoc) {
  return {
    font: await pdfDoc.embedFont(StandardFonts.Helvetica),
    bold: await pdfDoc.embedFont(StandardFonts.HelveticaBold),
  };
}

function centerText(page, text, y, size, font, black) {
  const w = font.widthOfTextAtSize(text, size);
  page.drawText(text, { x: (PAGE_W - w) / 2, y, size, font, color: black });
}

async function addPaketTrennblatt(pdfDoc, { kategorie }) {
  const page = pdfDoc.addPage([PAGE_W, PAGE_H]);
  const { font, bold } = await makeFonts(pdfDoc);
  const black = rgb(0, 0, 0);
  drawPatchFrame(page, black);

  centerText(page, 'Patchcode T', PAGE_H - 110, 10, font, black);
  centerText(page, 'Trennblatt', PAGE_H - 160, 16, font, black);
  centerText(page, 'Dokumentenpaket', PAGE_H - 185, 22, bold, black);

  let cursorY = PAGE_H - 220;
  if (kategorie) {
    const qrSize = 130;
    drawQr(page, kategorie, (PAGE_W - qrSize) / 2, cursorY - qrSize, qrSize, black);
    cursorY -= qrSize + 15;
    centerText(page, kategorie, cursorY, 9, font, black);
    cursorY -= 25;
    centerText(page, humanize(kategorie), cursorY, 14, font, black);
  }

  const qrSize2 = 110;
  const qrY = MARGIN + 90;
  drawQr(page, 'Patch_T', (PAGE_W - qrSize2) / 2, qrY, qrSize2, black);
  centerText(page, 'Patch_T', qrY - 15, 9, font, black);

  page.drawText('A 9464A/8.17', { x: MARGIN, y: MARGIN - 20, size: 7, font, color: black });
  return page;
}

async function addDokumentTrennblatt(pdfDoc, { doktyp }) {
  const page = pdfDoc.addPage([PAGE_W, PAGE_H]);
  const { font, bold } = await makeFonts(pdfDoc);
  const black = rgb(0, 0, 0);
  drawPatchFrame(page, black);

  centerText(page, 'Patchcode 2', PAGE_H - 110, 10, font, black);
  centerText(page, 'Trennblatt', PAGE_H - 160, 16, font, black);
  centerText(page, 'Dokument', PAGE_H - 185, 22, bold, black);

  let cursorY = PAGE_H - 220;
  if (doktyp) {
    const code = 'D' + String(doktyp.nr).padStart(3, '0');
    const qrSize = 130;
    drawQr(page, code, (PAGE_W - qrSize) / 2, cursorY - qrSize, qrSize, black);
    cursorY -= qrSize + 15;
    centerText(page, code, cursorY, 9, font, black);
    cursorY -= 25;
    centerText(page, `${doktyp.nr} - ${doktyp.name}`, cursorY, 14, font, black);
  }

  const qrSize2 = 110;
  const qrY = MARGIN + 90;
  drawQr(page, 'Patch_2', (PAGE_W - qrSize2) / 2, qrY, qrSize2, black);
  centerText(page, 'Patch_2', qrY - 15, 9, font, black);

  page.drawText('A 9461/8.17', { x: MARGIN, y: MARGIN - 20, size: 7, font, color: black });
  return page;
}

async function addZbhBlatt(pdfDoc) {
  const page = pdfDoc.addPage([PAGE_W, PAGE_H]);
  const { font, bold } = await makeFonts(pdfDoc);
  const black = rgb(0, 0, 0);
  drawPatchFrame(page, black);

  const qrSize = 110;
  drawQr(page, 'Patch_3', (PAGE_W - qrSize) / 2, PAGE_H - 110 - qrSize, qrSize, black);
  centerText(page, 'Patchcode 3', PAGE_H - 110 - qrSize - 20, 10, font, black);
  centerText(page, 'Trennblatt', PAGE_H - 110 - qrSize - 50, 16, font, black);
  centerText(page, 'Zusatzbehandlung', PAGE_H - 110 - qrSize - 75, 22, bold, black);

  const qrY = MARGIN + 90;
  drawQr(page, 'Patch_3', (PAGE_W - qrSize) / 2, qrY, qrSize, black);
  centerText(page, 'Patch_3', qrY - 15, 9, font, black);

  page.drawText('A 9462/6.25', { x: MARGIN, y: MARGIN - 20, size: 7, font, color: black });
  return page;
}

const LOREM =
  'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor ' +
  'incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud ' +
  'exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure ' +
  'dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. ' +
  'Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt ' +
  'mollit anim id est laborum.';

function wrapText(font, text, size, maxWidth) {
  const words = text.split(' ');
  const lines = [];
  let line = '';
  for (const word of words) {
    const test = line ? line + ' ' + word : word;
    if (font.widthOfTextAtSize(test, size) > maxWidth && line) {
      lines.push(line);
      line = word;
    } else {
      line = test;
    }
  }
  if (line) lines.push(line);
  return lines;
}

async function addDummySeite(pdfDoc, { kind, pageLabel }) {
  const page = pdfDoc.addPage([PAGE_W, PAGE_H]);
  const { font, bold } = await makeFonts(pdfDoc);
  const black = rgb(0, 0, 0);

  if (kind === 'stempel') {
    const text = pageLabel;
    const size = 48;
    const w = bold.widthOfTextAtSize(text, size);
    page.drawText(text, { x: (PAGE_W - w) / 2, y: PAGE_H / 2, size, font: bold, color: black });
  } else {
    const size = 11;
    const maxWidth = PAGE_W - 2 * MARGIN;
    const lines = wrapText(font, LOREM + ' ' + LOREM, size, maxWidth);
    let y = PAGE_H - MARGIN - 40;
    page.drawText(pageLabel, { x: MARGIN, y: PAGE_H - MARGIN, size: 10, font: bold, color: black });
    for (const line of lines) {
      page.drawText(line, { x: MARGIN, y, size, font, color: black });
      y -= size + 6;
      if (y < MARGIN) break;
    }
  }
  return page;
}

async function addItemNormal(out, item, loadSourceDoc) {
  if (item.type === 'paket-trenn') {
    await addPaketTrennblatt(out, { kategorie: item.kategorie });
  } else if (item.type === 'dokument-trenn') {
    await addDokumentTrennblatt(out, { doktyp: item.doktyp });
  } else if (item.type === 'zbh') {
    await addZbhBlatt(out);
  } else if (item.type === 'seite') {
    if (item.source.kind === 'generated') {
      await addDummySeite(out, item.source.dummy);
    } else if (item.source.kind === 'file') {
      const srcDoc = await loadSourceDoc(item.source.fileId);
      const [copied] = await out.copyPages(srcDoc, [item.source.pageIndex]);
      out.addPage(copied);
    }
  } else if (item.type === 'seite-stapel') {
    const srcDoc = await loadSourceDoc(item.source.fileId);
    const indices = Array.from({ length: item.source.pageCount }, (_, i) => i);
    const copiedPages = await out.copyPages(srcDoc, indices);
    for (const p of copiedPages) out.addPage(p);
  }
}

function clamp255(v) {
  return v < 0 ? 0 : v > 255 ? 255 : v;
}

function applyScannedLook(sourceCanvas) {
  const w = sourceCanvas.width;
  const h = sourceCanvas.height;

  const angleDeg = Math.random() * 6 - 3; // +/- 3 Grad
  const rotated = document.createElement('canvas');
  rotated.width = w;
  rotated.height = h;
  const rctx = rotated.getContext('2d');
  rctx.fillStyle = '#ffffff';
  rctx.fillRect(0, 0, w, h);
  rctx.translate(w / 2, h / 2);
  rctx.rotate((angleDeg * Math.PI) / 180);
  rctx.drawImage(sourceCanvas, -w / 2, -h / 2);

  const filtered = document.createElement('canvas');
  filtered.width = w;
  filtered.height = h;
  const fctx = filtered.getContext('2d');
  const brightness = 0.93 + Math.random() * 0.1;
  const contrast = 1.08 + Math.random() * 0.14;
  fctx.filter = `blur(0.6px) brightness(${brightness}) contrast(${contrast}) saturate(0.85)`;
  fctx.drawImage(rotated, 0, 0);
  fctx.filter = 'none';

  const imgData = fctx.getImageData(0, 0, w, h);
  const data = imgData.data;
  const noiseAmount = 22;
  for (let i = 0; i < data.length; i += 4) {
    const n = (Math.random() - 0.5) * noiseAmount;
    data[i] = clamp255(data[i] + n);
    data[i + 1] = clamp255(data[i + 1] + n);
    data[i + 2] = clamp255(data[i + 2] + n);
  }
  fctx.putImageData(imgData, 0, 0);

  // ungleichmäßige Ausleuchtung: leichter Helligkeitsverlauf über die Seite
  const lightAngle = Math.random() * Math.PI * 2;
  const lx = w / 2 + Math.cos(lightAngle) * w;
  const ly = h / 2 + Math.sin(lightAngle) * h;
  const lightGrad = fctx.createLinearGradient(w / 2 - Math.cos(lightAngle) * w, h / 2 - Math.sin(lightAngle) * h, lx, ly);
  lightGrad.addColorStop(0, 'rgba(0,0,0,0.05)');
  lightGrad.addColorStop(0.5, 'rgba(0,0,0,0)');
  lightGrad.addColorStop(1, 'rgba(0,0,0,0.08)');
  fctx.globalCompositeOperation = 'multiply';
  fctx.fillStyle = lightGrad;
  fctx.fillRect(0, 0, w, h);

  const grad = fctx.createRadialGradient(w / 2, h / 2, Math.min(w, h) * 0.3, w / 2, h / 2, Math.max(w, h) * 0.68);
  grad.addColorStop(0, 'rgba(0,0,0,0)');
  grad.addColorStop(1, 'rgba(0,0,0,0.22)');
  fctx.fillStyle = grad;
  fctx.fillRect(0, 0, w, h);
  fctx.globalCompositeOperation = 'source-over';

  addDustSpecks(fctx, w, h);

  return filtered;
}

function addDustSpecks(ctx, w, h) {
  // einzelne, unregelmäßig verteilte Staubkörner/Kratzer wie auf einer Scannerlinse/-glasscheibe
  const count = Math.round((w * h) / 28000);
  for (let i = 0; i < count; i++) {
    const x = Math.random() * w;
    const y = Math.random() * h;
    const r = 0.6 + Math.random() * Math.random() * 3.5; // meist winzig, selten größer
    const alpha = 0.2 + Math.random() * 0.45;
    const dark = Math.random() < 0.82;
    ctx.beginPath();
    if (Math.random() < 0.15) {
      // vereinzelt ein kleiner Kratzer statt eines runden Korns
      const len = 2 + Math.random() * 6;
      const ang = Math.random() * Math.PI * 2;
      ctx.strokeStyle = `rgba(50,50,50,${alpha})`;
      ctx.lineWidth = 0.6;
      ctx.moveTo(x, y);
      ctx.lineTo(x + Math.cos(ang) * len, y + Math.sin(ang) * len);
      ctx.stroke();
    } else {
      ctx.arc(x, y, r, 0, Math.PI * 2);
      ctx.fillStyle = dark ? `rgba(35,35,35,${alpha})` : `rgba(255,255,255,${alpha * 0.8})`;
      ctx.fill();
    }
  }
}

function dataUrlToBytes(dataUrl) {
  const base64 = dataUrl.split(',')[1];
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

async function rasterizeAndEmbed(out, pdfjsDoc, pageNumber) {
  const page = await pdfjsDoc.getPage(pageNumber);
  const baseViewport = page.getViewport({ scale: 1 });
  const scale = 1.8;
  const viewport = page.getViewport({ scale });
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(viewport.width);
  canvas.height = Math.round(viewport.height);
  await page.render({ canvasContext: canvas.getContext('2d'), viewport }).promise;

  const processed = applyScannedLook(canvas);
  const jpegBytes = dataUrlToBytes(processed.toDataURL('image/jpeg', 0.72));
  const image = await out.embedJpg(jpegBytes);
  const outPage = out.addPage([baseViewport.width, baseViewport.height]);
  outPage.drawImage(image, { x: 0, y: 0, width: baseViewport.width, height: baseViewport.height });
}

async function addItemScanned(out, item, loadSourceDoc, loadSourcePdfJsDoc) {
  if (item.type === 'seite-stapel') {
    const pdfjsDoc = await loadSourcePdfJsDoc(item.source.fileId);
    for (let i = 0; i < item.source.pageCount; i++) {
      await rasterizeAndEmbed(out, pdfjsDoc, i + 1);
    }
    return;
  }
  if (item.type === 'seite' && item.source.kind === 'file') {
    const pdfjsDoc = await loadSourcePdfJsDoc(item.source.fileId);
    await rasterizeAndEmbed(out, pdfjsDoc, item.source.pageIndex + 1);
    return;
  }

  const temp = await PDFDocument.create();
  if (item.type === 'paket-trenn') {
    await addPaketTrennblatt(temp, { kategorie: item.kategorie });
  } else if (item.type === 'dokument-trenn') {
    await addDokumentTrennblatt(temp, { doktyp: item.doktyp });
  } else if (item.type === 'zbh') {
    await addZbhBlatt(temp);
  } else if (item.type === 'seite' && item.source.kind === 'generated') {
    await addDummySeite(temp, item.source.dummy);
  } else {
    return;
  }
  const tempBytes = await temp.save();
  const pdfjsDoc = await window.pdfjsLib.getDocument({ data: tempBytes }).promise;
  await rasterizeAndEmbed(out, pdfjsDoc, 1);
}

async function buildFinalPdf(items, loadSourceDoc, options = {}) {
  const { scanMode = false, loadSourcePdfJsDoc = null, onProgress = null } = options;
  const out = await PDFDocument.create();
  let done = 0;
  for (const item of items) {
    if (item.type === 'fehlend') {
      // wird bewusst ausgelassen, Datei fehlt im Quellverzeichnis
    } else if (scanMode) {
      await addItemScanned(out, item, loadSourceDoc, loadSourcePdfJsDoc);
    } else {
      await addItemNormal(out, item, loadSourceDoc);
    }
    done++;
    if (onProgress) onProgress(done, items.length);
  }
  return out.save();
}

return { PAGE_W, PAGE_H, humanize, addPaketTrennblatt, addDokumentTrennblatt, addZbhBlatt, addDummySeite, buildFinalPdf };
})();
