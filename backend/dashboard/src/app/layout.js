export const metadata = {
  title: 'GameArena Admin Dashboard',
  description: 'Manage GameArena tournament platform',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body style={{ margin: 0, background: '#0a0a0a' }}>{children}</body>
    </html>
  );
}
