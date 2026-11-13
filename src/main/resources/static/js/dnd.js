// Generic drag & drop for HTMX pages. You never need to edit this file:
// the markup is generated from Kotlin with dragItem(...) / dropZone(...) (ui/core/DragDrop.kt).
//
//   data-drag="<value>"        element can be dragged; value is sent as `item`
//   data-drop="<url>"          drop zone; on drop it POSTs `item` (+ data-drop-vals) via htmx
//   data-drop-vals='{"k":"v"}' extra values sent with the drop
//   data-upload                file drop box: files dropped anywhere on it go into its <input type=file>
//
// Uses event delegation, so it keeps working for content swapped in by HTMX.
(function () {
  let dragged = null;

  const zoneOf = (e) => dragged && e.target.closest && e.target.closest("[data-drop]");
  const clear = (except) =>
    document.querySelectorAll(".drag-over").forEach((n) => n !== except && n.classList.remove("drag-over"));

  document.addEventListener("dragstart", (e) => {
    const el = e.target.closest && e.target.closest("[data-drag]");
    if (!el) return;
    dragged = el;
    el.classList.add("dragging");
    document.body.classList.add("is-dragging");
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", el.dataset.drag);
  });

  document.addEventListener("dragend", () => {
    if (dragged) dragged.classList.remove("dragging");
    document.body.classList.remove("is-dragging");
    dragged = null;
    clear();
  });

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
    clear();
    const values = Object.assign({ item: dragged.dataset.drag }, JSON.parse(zone.dataset.dropVals || "{}"));
    htmx.ajax("POST", zone.dataset.drop, { values: values, swap: "none" });
  });

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
