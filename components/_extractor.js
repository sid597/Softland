/**
 * Browser DOM extractor — runs via Claude-in-Chrome javascript_tool.
 * Walks a DOM subtree, captures computed styles + bounds, returns a JSON tree
 * matching the Design IR shape for ingestion by _css_parsers and _compiler.
 *
 * Usage: extractComponent('button.my-class') or extractComponent('#card-id')
 */
/**
 * Auto-detect the most likely "main content" container.
 * Tries semantic landmarks first, then falls back to the largest body child.
 */
function autoDetectSelector() {
  var candidates = ['main', 'article', '[role="main"]', '.main-content', '#content'];
  for (var i = 0; i < candidates.length; i++) {
    var el = document.querySelector(candidates[i]);
    if (el && el.children.length > 0 && el.getBoundingClientRect().height > 50) return candidates[i];
  }
  // Largest body child, excluding nav/header/footer
  var skip = {NAV:1, HEADER:1, FOOTER:1, SCRIPT:1, STYLE:1, NOSCRIPT:1};
  var best = null, bestArea = 0;
  for (var j = 0; j < document.body.children.length; j++) {
    var child = document.body.children[j];
    if (skip[child.tagName]) continue;
    var r = child.getBoundingClientRect();
    if (r.width * r.height > bestArea) { bestArea = r.width * r.height; best = child; }
  }
  if (best) {
    if (best.id) return '#' + best.id;
    return 'body > :nth-child(' + (Array.from(document.body.children).indexOf(best) + 1) + ')';
  }
  return 'body';
}

function extractComponent(selector) {
  if (!selector) selector = autoDetectSelector();
  const SKIP_TAGS = new Set(['SCRIPT', 'STYLE', 'META', 'LINK', 'NOSCRIPT', 'BR', 'HR']);

  const STYLE_PROPS = [
    'backgroundColor', 'backgroundImage',
    'borderTopWidth', 'borderRightWidth', 'borderBottomWidth', 'borderLeftWidth',
    'borderTopColor', 'borderRightColor', 'borderBottomColor', 'borderLeftColor',
    'borderTopLeftRadius', 'borderTopRightRadius', 'borderBottomRightRadius', 'borderBottomLeftRadius',
    'boxShadow',
    'fontSize', 'fontWeight', 'fontFamily', 'color',
    'display', 'flexDirection', 'alignItems', 'justifyContent', 'gap',
    'paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft',
    'overflow', 'opacity', 'visibility'
  ];

  const root = document.querySelector(selector);
  if (!root) return JSON.stringify({ error: 'Element not found: ' + selector });

  const rootRect = root.getBoundingClientRect();

  function walkNode(el) {
    if (SKIP_TAGS.has(el.tagName)) return null;

    const cs = getComputedStyle(el);
    if (cs.display === 'none' || cs.visibility === 'hidden') return null;

    const rect = el.getBoundingClientRect();

    // Capture style properties
    const styles = {};
    for (const prop of STYLE_PROPS) {
      styles[prop] = cs[prop];
    }

    // Relative bounds (to root element)
    const bounds = {
      x: Math.round(rect.left - rootRect.left),
      y: Math.round(rect.top - rootRect.top),
      w: Math.round(rect.width),
      h: Math.round(rect.height)
    };

    // Gather children
    const children = [];
    for (const child of el.children) {
      const node = walkNode(child);
      if (node) children.push(node);
    }

    // Text content (only on leaf elements with no element children)
    let textContent = null;
    if (children.length === 0) {
      const text = el.textContent?.trim();
      if (text && text.length > 0) textContent = text;
    }

    return {
      tag: el.tagName.toLowerCase(),
      bounds,
      styles,
      textContent,
      children: children.length > 0 ? children : undefined
    };
  }

  const tree = walkNode(root);

  // Post-pass: collapse pure wrapper divs
  function collapseWrappers(node) {
    if (!node) return node;
    if (node.children) {
      node.children = node.children.map(collapseWrappers);
    }

    // Collapse if: single child, div/span, no visual styling
    if (node.children && node.children.length === 1 && !node.textContent) {
      const s = node.styles;
      const isTransparent = !s.backgroundColor ||
        s.backgroundColor === 'rgba(0, 0, 0, 0)' ||
        s.backgroundColor === 'transparent';
      const noBorder = (!s.borderTopWidth || parseFloat(s.borderTopWidth) === 0) &&
        (!s.borderRightWidth || parseFloat(s.borderRightWidth) === 0);
      const noShadow = !s.boxShadow || s.boxShadow === 'none';
      const noBackground = !s.backgroundImage || s.backgroundImage === 'none';
      const isWrapper = (node.tag === 'div' || node.tag === 'span');

      if (isWrapper && isTransparent && noBorder && noShadow && noBackground) {
        // Promote the single child, preserving the wrapper's bounds if child has none
        const child = node.children[0];
        if (!child.bounds || (child.bounds.w === 0 && child.bounds.h === 0)) {
          child.bounds = node.bounds;
        }
        return child;
      }
    }

    return node;
  }

  const collapsed = collapseWrappers(tree);

  // Capture source HTML for the preview overlay
  const sourceHTML = root.outerHTML;

  return JSON.stringify({
    version: 1,
    sourceUrl: window.location.href,
    rootBounds: { w: Math.round(rootRect.width), h: Math.round(rootRect.height) },
    tree: collapsed,
    sourceHTML: sourceHTML
  }, null, 2);
}
