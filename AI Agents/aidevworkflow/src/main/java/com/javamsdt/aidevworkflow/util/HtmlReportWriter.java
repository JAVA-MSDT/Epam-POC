package com.javamsdt.aidevworkflow.util;

/**
 * Wraps LLM-generated HTML content in a standard page shell and writes it to disk.
 * The LLM is responsible for producing the body content; this class handles the
 * surrounding boilerplate (doctype, head, styles, navigation).
 */
public final class HtmlReportWriter {

    private HtmlReportWriter() {
    }

    /**
     * Wraps bodyHtml in a full HTML page and writes it to filePath.
     * Returns the absolute path of the written file.
     */
    public static String write(String filePath, String title, String bodyHtml) {
        String page = buildPage(title, bodyHtml);
        FileSystemUtil.writeFile(filePath, page);
        return filePath;
    }

    /**
     * Convenience overload: derives the file path from reportFolderPath + fileName.
     */
    public static String write(String reportFolderPath, String fileName, String title, String bodyHtml) {
        String filePath = reportFolderPath + "/" + fileName;
        return write(filePath, title, bodyHtml);
    }

    private static String buildPage(String title, String bodyHtml) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>%s</title>
                  <style>
                    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                           background: #0d1117; color: #c9d1d9; line-height: 1.6; padding: 2rem; }
                    h1, h2, h3 { color: #e6edf3; margin: 1.5rem 0 0.5rem; }
                    h1 { font-size: 1.8rem; border-bottom: 2px solid #30363d; padding-bottom: 0.5rem; }
                    h2 { font-size: 1.3rem; color: #58a6ff; }
                    h3 { font-size: 1.1rem; color: #79c0ff; }
                    p  { margin: 0.5rem 0; }
                    a  { color: #58a6ff; }
                    table { width: 100%%; border-collapse: collapse; margin: 1rem 0; font-size: 0.9rem; }
                    th { background: #161b22; color: #8b949e; text-align: left; padding: 0.5rem 0.75rem;
                         border-bottom: 1px solid #30363d; }
                    td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #21262d; }
                    tr:hover td { background: #161b22; }
                    pre, code { font-family: "SFMono-Regular", Consolas, monospace;
                                background: #161b22; border-radius: 6px; }
                    pre  { padding: 1rem; overflow-x: auto; margin: 0.75rem 0;
                           border: 1px solid #30363d; }
                    code { padding: 0.15rem 0.4rem; font-size: 0.875em; }
                    ul, ol { padding-left: 1.5rem; margin: 0.5rem 0; }
                    li { margin: 0.25rem 0; }
                    .badge { display: inline-block; padding: 0.2rem 0.6rem; border-radius: 12px;
                             font-size: 0.75rem; font-weight: 600; }
                    .high   { background: #3d1a1a; color: #f85149; }
                    .medium { background: #2d2a1a; color: #e3b341; }
                    .low    { background: #1a2d1a; color: #56d364; }
                    .section { background: #161b22; border: 1px solid #30363d; border-radius: 8px;
                               padding: 1.25rem; margin: 1rem 0; }
                  </style>
                </head>
                <body>
                  <h1>%s</h1>
                  %s
                </body>
                </html>
                """.formatted(title, title, bodyHtml);
    }
}
