import React from 'react'
import { FiDownload, FiPlay } from 'react-icons/fi'
import { FaGamepad, FaBolt, FaTrophy } from 'react-icons/fa'

export default function Hero() {
  return (
    <section className="relative min-h-screen flex items-center justify-center hero-gradient overflow-hidden">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-20 left-10 w-72 h-72 bg-[#6C5CE7]/10 rounded-full blur-3xl animate-float" />
        <div className="absolute bottom-20 right-10 w-96 h-96 bg-[#00D2FF]/5 rounded-full blur-3xl animate-float" style={{ animationDelay: '2s' }} />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-[#6C5CE7]/5 rounded-full blur-3xl" />

        <div className="absolute top-32 right-1/4 text-6xl opacity-10 animate-float" style={{ animationDelay: '1s' }}>
          <FaGamepad />
        </div>
        <div className="absolute bottom-32 left-1/4 text-5xl opacity-10 animate-float" style={{ animationDelay: '3s' }}>
          <FaTrophy />
        </div>
      </div>

      <div className="relative z-10 max-w-5xl mx-auto px-6 text-center pt-24">
        <div className="inline-flex items-center gap-2 bg-[#6C5CE7]/10 border border-[#6C5CE7]/20 rounded-full px-4 py-1.5 mb-8">
          <FaBolt className="text-[#6C5CE7]" size={14} />
          <span className="text-sm text-[#6C5CE7] font-medium">The Ultimate Mobile Gaming Platform</span>
        </div>

        <h1 className="text-5xl md:text-7xl lg:text-8xl font-black text-white leading-tight mb-6">
          Compete.
          <br />
          <span className="gradient-text">Boost.</span>
          <br />
          Win.
        </h1>

        <p className="text-lg md:text-xl text-gray-400 max-w-2xl mx-auto mb-10 leading-relaxed">
          Optimize your phone for peak gaming performance. Join tournaments,
          compete against players worldwide, and win real money.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-16">
          <a href="#download" className="download-btn flex items-center gap-3 px-8 py-4 rounded-full text-white font-bold text-lg">
            <FiDownload size={20} />
            Download Free
          </a>
          <a href="#features" className="flex items-center gap-3 px-8 py-4 rounded-full border border-white/10 text-white font-semibold text-lg hover:bg-white/5 transition-all">
            <FiPlay size={18} />
            See How It Works
          </a>
        </div>

        <div className="grid grid-cols-3 gap-8 max-w-lg mx-auto">
          <div>
            <p className="text-3xl font-black gradient-text">10K+</p>
            <p className="text-xs text-gray-500 mt-1">Active Players</p>
          </div>
          <div>
            <p className="text-3xl font-black gradient-text">500+</p>
            <p className="text-xs text-gray-500 mt-1">Tournaments</p>
          </div>
          <div>
            <p className="text-3xl font-black gradient-text">KES 2M+</p>
            <p className="text-xs text-gray-500 mt-1">Prizes Won</p>
          </div>
        </div>
      </div>

      <div className="absolute bottom-8 left-1/2 -translate-x-1/2 animate-bounce">
        <div className="w-6 h-10 rounded-full border-2 border-white/20 flex items-start justify-center p-1.5">
          <div className="w-1.5 h-3 bg-white/40 rounded-full" />
        </div>
      </div>
    </section>
  )
}
