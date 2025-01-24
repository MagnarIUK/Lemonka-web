import '@vaadin/common-frontend/ConnectionIndicator.js';
import '@vaadin/polymer-legacy-adapter/style-modules.js';
import '@vaadin/vaadin-lumo-styles/color-global.js';
import '@vaadin/vaadin-lumo-styles/typography-global.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '6efb0d5523d2c49a1c065e07a1919e18bb0d22470d5caebfc1dab848a98124ea') {
    pending.push(import('./chunks/chunk-f858cf98951556bc44579cfd66907eceae7b68ab6e803a7e6ba7e2e469a17961.js'));
  }
  if (key === '6f9fedd0af3bae15eff7f20ab8057c2f9f76b4a00928a8c053a5d57f1b420b8b') {
    pending.push(import('./chunks/chunk-f858cf98951556bc44579cfd66907eceae7b68ab6e803a7e6ba7e2e469a17961.js'));
  }
  if (key === '2cbf72f4f54ca174f999d70bd7473fd6f538fdc55a8c0a2c15b5384abf10c0a9') {
    pending.push(import('./chunks/chunk-f858cf98951556bc44579cfd66907eceae7b68ab6e803a7e6ba7e2e469a17961.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}