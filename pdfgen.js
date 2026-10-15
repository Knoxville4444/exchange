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

async function buildFinalPdf(items, loadSourceDoc) {
  const out = await PDFDocument.create();
  for (const item of items) {
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
  return out.save();
}

return { PAGE_W, PAGE_H, humanize, addPaketTrennblatt, addDokumentTrennblatt, addZbhBlatt, addDummySeite, buildFinalPdf };
})();
