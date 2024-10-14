document.addEventListener('DOMContentLoaded', (event) => {
  const form = document.getElementById('uploadForm');
  const fileInput = document.getElementById('fileInput');
  const statusDiv = document.getElementById('status');

  console.log("attaching stuff to stuff", event);
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const file = fileInput.files[0];
    if (!file) {
      statusDiv.textContent = 'Please select a file first.';
      return;
    }

    statusDiv.textContent = 'Compressing and uploading file...';

    try {
      await uploadGzippedFile(file);
      statusDiv.textContent = 'File uploaded successfully';
    } catch (error) {
      statusDiv.textContent = `Upload failed: ${error.message}`;
    }
  });
});

async function uploadGzippedFile(file) {
  if (typeof CompressionStream === 'undefined') {
    throw new Error('CompressionStream is not supported in this browser');
  }

  const fileStream = file.stream();
  const compressedStream = fileStream.pipeThrough(new CompressionStream('gzip'));
  const compressedBlob = await new Response(compressedStream).blob();

  const response = await fetch('/profiles/upload-collapsed-file', {
    method: 'POST',
    body: compressedBlob,
    headers: {
      'Content-Type': 'application/gzip',
      'Content-Encoding': 'gzip',
      'X-Filename': file.name  // We'll send the original filename as a custom header
    }
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const responseText = await response.text();
  console.log('Server response:', responseText);
}
