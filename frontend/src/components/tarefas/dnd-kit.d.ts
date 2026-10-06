import type { JSX as ReactJSX } from "react";
// O dnd-kit publica JSX.Element global; React 19 mantém o mesmo contrato em React.JSX.
declare global {
  namespace JSX {
    type Element = ReactJSX.Element;
    type IntrinsicElements = ReactJSX.IntrinsicElements;
  }
}
