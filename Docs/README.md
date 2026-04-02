To generate SVG diagrams from Mermaid syntax, use Mermaid CLI.

1. Install Mermaid CLI globally using npm:

   ```bash
   npm install -g @mermaid-js/mermaid-cli
   ```

2. Generate an SVG diagram from a Mermaid file:

   ```bash
   mmdc -i input.mmd -o output.svg -t dark -b transparent
   ```
