// Generic drag & drop for HTMX pages. You never need to edit this file:
// the markup is generated from Kotlin with dragItem(...) / dropZone(...) / uploadDropZone() (ui/core/DragDrop.kt).
//
//   data-drag="<value>"        element can be dragged; value is sent as `item`
//   data-drop="<url>"          drop zone; on drop it POSTs `item` (+ data-drop-vals) via htmx
//   data-drop-vals='{"k":"v"}' extra values sent with the drop
//   data-upload                file drop box: files dropped anywhere on it go into its <input type=file>
//
// Works with a mouse (HTML5 drag events) and on phones (long-press, then drag).
// Uses event delegation, so it keeps working for content swapped in by HTMX.
(function () {
  let dragged = null;

  const clear = (except) =>
    document.querySelectorAll(".drag-over").forEach((n) => n !== except && n.classList.remove("drag-over"));

  function begin(el) {
    dragged = el;
    el.classList.add("dragging");
    document.body.classList.add("is-dragging");
  }

  function end() {
    if (dragged) dragged.classList.remove("dragging");
    document.body.classList.remove("is-dragging");
    dragged = null;
    clear();
  }

  function send(zone, item) {
    const values = Object.assign({ item: item }, JSON.parse(zone.dataset.dropVals || "{}"));
    htmx.ajax("POST", zone.dataset.drop, { values: values, swap: "none" });
  }

  // ---- mouse (HTML5 drag & drop) ----
  const zoneOf = (e) => dragged && e.target.closest && e.target.closest("[data-drop]");

  document.addEventListener("dragstart", (e) => {
    const el = e.target.closest && e.target.closest("[data-drag]");
    if (!el) return;
    if (touch) { e.preventDefault(); return; } // a touch drag is already running
    begin(el);
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", el.dataset.drag);
  });

  document.addEventListener("dragend", end);

  document.addEventListener("dragover", (e) => {
    const zone = zoneOf(e);
    if (!zone) return;
    e.preventDefault();
    clear(zone);
    zone.classList.add("drag-over");
  });

  document.addEventListener("dragleave", (e) => {
    const zone = zoneOf(e);
    if (zone && !zone.contains(e.relatedTarget)) zone.classList.remove("drag-over");
  });

  document.addEventListener("drop", (e) => {
    const zone = zoneOf(e);
    if (!zone) return;
    e.preventDefault();
    const item = dragged.dataset.drag;
    end();
    send(zone, item);
  });

  // ---- touch: long-press to pick up, drag, lift to drop ----
  const LONG_PRESS_MS = 300;
  const MOVE_TOLERANCE = 10; // px of movement that still counts as "holding still"
  let touch = null; // { el, x, y, timer, active, ghost, zone }

  document.addEventListener("touchstart", (e) => {
    const el = e.target.closest && e.target.closest("[data-drag]");
    if (!el || e.touches.length !== 1) return;
    const t = e.touches[0];
    touch = { el: el, x: t.clientX, y: t.clientY, active: false, ghost: null, zone: null };
    touch.timer = setTimeout(pickUp, LONG_PRESS_MS);
  }, { passive: true });

  function pickUp() {
    if (!touch) return;
    touch.active = true;
    begin(touch.el);
    const r = touch.el.getBoundingClientRect();
    const ghost = touch.el.cloneNode(true);
    ghost.classList.remove("dragging");
    ghost.classList.add("drag-ghost");
    ghost.style.width = r.width + "px";
    document.body.appendChild(ghost);
    touch.ghost = ghost;
    moveGhost(touch.x, touch.y);
    if (navigator.vibrate) navigator.vibrate(15);
  }

  function moveGhost(x, y) {
    touch.ghost.style.transform = "translate(" + (x - touch.ghost.offsetWidth / 2) + "px," + (y - 28) + "px)";
  }

  document.addEventListener("touchmove", (e) => {
    if (!touch) return;
    const t = e.touches[0];
    if (!touch.active) {
      // Moved before the long-press finished: it's a scroll, not a drag.
      if (Math.hypot(t.clientX - touch.x, t.clientY - touch.y) > MOVE_TOLERANCE) cancelTouch();
      return;
    }
    e.preventDefault(); // stop the page from scrolling while dragging
    moveGhost(t.clientX, t.clientY);
    const under = document.elementFromPoint(t.clientX, t.clientY);
    const zone = under && under.closest("[data-drop]");
    clear(zone);
    if (zone) zone.classList.add("drag-over");
    touch.zone = zone;
  }, { passive: false });

  document.addEventListener("touchend", () => {
    if (!touch) return;
    const t = touch;
    cancelTouch();
    if (t.active && t.zone) send(t.zone, t.el.dataset.drag);
  });

  document.addEventListener("touchcancel", cancelTouch);

  // Long-press would otherwise open the browser's context menu / text selection.
  document.addEventListener("contextmenu", (e) => {
    if (touch || (e.target.closest && e.target.closest("[data-drag]"))) e.preventDefault();
  });

  function cancelTouch() {
    if (!touch) return;
    clearTimeout(touch.timer);
    if (touch.ghost) touch.ghost.remove();
    if (touch.active) end();
    touch = null;
  }

  // ---- files from the computer ----
  // Without this the browser only accepts files dropped exactly on the "Browse" button,
  // and opens the file (leaving the page) when it's dropped anywhere else.
  const isFiles = (e) => !dragged && e.dataTransfer && Array.from(e.dataTransfer.types || []).includes("Files");
  const uploadOf = (e) => e.target.closest && e.target.closest("[data-upload]");

  document.addEventListener("dragover", (e) => {
    if (!isFiles(e)) return;
    e.preventDefault(); // never let the browser open the file
    const box = uploadOf(e);
    e.dataTransfer.dropEffect = box ? "copy" : "none";
    clear(box);
    if (box) box.classList.add("drag-over");
  });

  document.addEventListener("dragleave", (e) => {
    const box = isFiles(e) && uploadOf(e);
    if (box && !box.contains(e.relatedTarget)) box.classList.remove("drag-over");
  });

  document.addEventListener("drop", (e) => {
    if (!isFiles(e)) return;
    e.preventDefault();
    clear();
    const box = uploadOf(e);
    const input = box && box.querySelector("input[type=file]");
    if (!input) return;
    const files = Array.from(e.dataTransfer.files);
    const picked = new DataTransfer();
    (input.multiple ? files : files.slice(0, 1)).forEach((f) => picked.items.add(f));
    input.files = picked.files;
    input.dispatchEvent(new Event("change", { bubbles: true })); // e.g. auto-upload forms react to this
  });
})();
