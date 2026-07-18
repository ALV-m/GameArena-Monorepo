import React from 'react'
import { FaGithub, FaTwitter, FaDiscord, FaEnvelope } from 'react-icons/fa'

const footerLinks = {
  Product: [
    { label: 'Features', href: '#features' },
    { label: 'Leaderboard', href: '#leaderboard' },
    { label: 'Download', href: '#download' },
    { label: 'Changelog', href: 'https://github.com/ALV-m/GameArena/releases' },
  ],
  Community: [
    { label: 'Discord', href: 'https://discord.gg/gamearena', external: true },
    { label: 'Twitter', href: 'https://twitter.com/gamearena', external: true },
    { label: 'GitHub', href: 'https://github.com/ALV-m/GameArena', external: true },
    { label: 'Tournaments', href: '#features' },
  ],
  Support: [
    { label: 'FAQ', href: '#' },
    { label: 'Contact', href: 'mailto:support@gamearena.app' },
    { label: 'Privacy Policy', href: '#' },
    { label: 'Terms of Service', href: '#' },
  ],
}

export default function Footer() {
  return (
    <footer className="bg-[#0a0a0f] border-t border-white/5 py-16 px-6">
      <div className="max-w-7xl mx-auto">
        <div className="grid md:grid-cols-5 gap-12 mb-12">
          <div className="md:col-span-2">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[#6C5CE7] to-[#00D2FF] flex items-center justify-center font-black text-white text-lg">
                G
              </div>
              <span className="text-xl font-bold text-white">GameArena</span>
            </div>
            <p className="text-gray-500 text-sm leading-relaxed mb-6 max-w-sm">
              The ultimate mobile gaming platform. Boost performance, compete in tournaments, and win real money. Built for gamers, by gamers.
            </p>
            <div className="flex gap-3">
              <a href="https://github.com/ALV-m/GameArena" target="_blank" rel="noopener noreferrer"
                className="w-9 h-9 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/10 transition-all">
                <FaGithub size={16} />
              </a>
              <a href="https://twitter.com/gamearena" target="_blank" rel="noopener noreferrer"
                className="w-9 h-9 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/10 transition-all">
                <FaTwitter size={16} />
              </a>
              <a href="https://discord.gg/gamearena" target="_blank" rel="noopener noreferrer"
                className="w-9 h-9 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/10 transition-all">
                <FaDiscord size={16} />
              </a>
              <a href="mailto:support@gamearena.app"
                className="w-9 h-9 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-gray-400 hover:text-white hover:bg-white/10 transition-all">
                <FaEnvelope size={16} />
              </a>
            </div>
          </div>

          {Object.entries(footerLinks).map(([category, links]) => (
            <div key={category}>
              <h4 className="text-white font-semibold text-sm mb-4">{category}</h4>
              <ul className="space-y-3">
                {links.map(link => (
                  <li key={link.label}>
                    <a href={link.href} target={link.external ? '_blank' : undefined} rel={link.external ? 'noopener noreferrer' : undefined}
                      className="text-gray-500 hover:text-white text-sm transition-colors">
                      {link.label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="border-t border-white/5 pt-8 flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-gray-600 text-xs">
            &copy; {new Date().getFullYear()} GameArena. All rights reserved. Made in Kenya.
          </p>
          <a href="/admin" className="text-gray-700 hover:text-gray-500 text-xs transition-colors">
            Admin Dashboard
          </a>
        </div>
      </div>
    </footer>
  )
}
