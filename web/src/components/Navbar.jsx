import React, { useState, useEffect } from 'react'
import { HiMenu, HiX } from 'react-icons/hi'

export default function Navbar() {
  const [scrolled, setScrolled] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 50)
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const navLinks = [
    { label: 'Features', href: '#features' },
    { label: 'Leaderboard', href: '#leaderboard' },
    { label: 'Videos', href: '#videos' },
    { label: 'Download', href: '#download' },
  ]

  return (
    <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${scrolled ? 'bg-[#0a0a0f]/95 backdrop-blur-md border-b border-white/5' : 'bg-transparent'}`}>
      <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
        <a href="#" className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[#6C5CE7] to-[#00D2FF] flex items-center justify-center font-black text-white text-lg">
            G
          </div>
          <span className="text-xl font-bold text-white">GameArena</span>
        </a>

        <div className="hidden md:flex items-center gap-8">
          {navLinks.map(link => (
            <a key={link.label} href={link.href} className="text-gray-400 hover:text-white transition-colors text-sm font-medium">
              {link.label}
            </a>
          ))}
          <a href="#download" className="download-btn px-5 py-2.5 rounded-full text-white font-semibold text-sm">
            Download App
          </a>
          <a href="/admin" className="text-gray-500 hover:text-gray-300 transition-colors text-xs">
            Admin
          </a>
        </div>

        <button className="md:hidden text-white" onClick={() => setMobileOpen(!mobileOpen)}>
          {mobileOpen ? <HiX size={24} /> : <HiMenu size={24} />}
        </button>
      </div>

      {mobileOpen && (
        <div className="md:hidden bg-[#0a0a0f]/98 backdrop-blur-md border-t border-white/5 px-6 py-4">
          {navLinks.map(link => (
            <a key={link.label} href={link.href} onClick={() => setMobileOpen(false)} className="block py-3 text-gray-400 hover:text-white transition-colors">
              {link.label}
            </a>
          ))}
          <a href="#download" className="download-btn block text-center mt-4 px-5 py-3 rounded-full text-white font-semibold">
            Download App
          </a>
        </div>
      )}
    </nav>
  )
}
