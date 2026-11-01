// src/utils/exportUtils.js

// Export data as CSV
export const exportToCSV = (data, fileName = "report") => {
  if (!Array.isArray(data) || data.length === 0) {
    alert("No data available to export.");
    return;
  }

  const headers = Object.keys(data[0]);

  const escapeCSV = (value) => {
    if (value === null || value === undefined) return '""';

    const formattedValue =
      typeof value === "object" ? JSON.stringify(value) : String(value);

    return `"${formattedValue.replace(/"/g, '""')}"`;
  };

  const csvRows = [
    headers.map(escapeCSV).join(","),
    ...data.map((row) =>
      headers.map((header) => escapeCSV(row[header])).join(","),
    ),
  ];

  const csvContent = "\uFEFF" + csvRows.join("\n");

  const blob = new Blob([csvContent], {
    type: "text/csv;charset=utf-8;",
  });

  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = `${fileName}.csv`;

  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(url);
};

// Export data as printable PDF
export const exportToPDF = (title, data) => {
  if (!Array.isArray(data) || data.length === 0) {
    alert("No data available to export.");
    return;
  }

  const headers = Object.keys(data[0]);

  const escapeHTML = (value) =>
    String(value ?? "")
      .replace(/&/g, "&")
      .replace(/</g, "<")
      .replace(/>/g, ">")
      .replace(/"/g, '""')
      .replace(/'/g, "'");

  const tableHeaders = headers
    .map((header) => `<th>${escapeHTML(header)}</th>`)
    .join("");

  const tableRows = data
    .map(
      (row) => `        <tr>
          ${headers
            .map((header) => `<td>${escapeHTML(row[header])}</td>`)
            .join("")}         </tr>
      `,
    )
    .join("");

  const printWindow = window.open("", "_blank");

  if (!printWindow) {
    alert("Please allow pop-ups to export the PDF.");
    return;
  }

  printWindow.document
    .write(` <!DOCTYPE html> <html> <head> <title>${escapeHTML(title)}</title>


    <style>
      body {
        font-family: Arial, sans-serif;
        padding: 30px;
        color: #222;
      }

      h1 {
        margin-bottom: 8px;
        color: #172554;
      }

      .date {
        color: #666;
        margin-bottom: 25px;
        font-size: 13px;
      }

      table {
        width: 100%;
        border-collapse: collapse;
        margin-top: 20px;
      }

      th, td {
        border: 1px solid #ddd;
        padding: 10px;
        text-align: left;
        font-size: 12px;
      }

      th {
        background: #eef2ff;
        color: #172554;
      }

      tr:nth-child(even) {
        background: #f8fafc;
      }

      @media print {
        body {
          padding: 0;
        }
      }
    </style>
  </head>

  <body>
    <h1>FinNexa</h1>
    <h2>${escapeHTML(title)}</h2>

    <p class="date">
      Generated on: ${new Date().toLocaleString()}
    </p>

    <table>
      <thead>
        <tr>${tableHeaders}</tr>
      </thead>

      <tbody>
        ${tableRows}
      </tbody>
    </table>

    <script>
      window.onload = function () {
        window.print();
      };
    </script>
  </body>
</html>


`);

  printWindow.document.close();
};
