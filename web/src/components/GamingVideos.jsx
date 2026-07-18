import React from 'react'
import { FaPlay, FaExternalLinkAlt } from 'react-icons/fa'

const videos = [
  {
    title: 'GameArena Boost in Action',
    description: 'Watch how our Game Booster suspends background apps and maximizes FPS for Free Fire.',
    thumbnail: null,
    duration: '2:34',
    category: 'Tutorial',
    link: 'https://www.youtube.com/results?search_query=mobile+gaming+boost',
  },
  {
    title: 'Tournament Finals — KES 50,000 Prize Pool',
    description: 'Full highlights from the GameArena Championship Finals. 32 players, 1 winner.',
    thumbnail: null,
    duration: '8:12',
    category: 'Highlights',
    link: 'https://www.youtube.com/results?search_query=mobile+esports+tournament',
  },
  {
    title: 'Overlay Deep Dive — 8 Real-Time Panels',
    description: 'Complete walkthrough of the in-game overlay: FPS, CPU, GPU, wallet, chat, and more.',
    thumbnail: null,
    duration: '5:47',
    category: 'Feature Walkthrough',
    link: 'https://www.youtube.com/results?search_query=gaming+overlay+mobile',
  },
]

const ads = [
  { title: 'Deposit Bonus', description: 'First M-Pesa deposit gets 20% bonus up to KES 1,000', cta: 'Claim Now', color: '#22C55E' },
  { title: 'Refer a Friend', description: 'Both get KES 100 when they complete their first tournament', cta: 'Invite Now', color: '#6C5CE7' },
  { title: 'Weekend Tournament', description: 'KES 25,000 prize pool this Saturday. Entry fee: KES 200', cta: 'Register', color: '#FFD700' },
]

export default function GamingVideos() {
  return (
    <section id="videos" className="py-24 px-6">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <p className="text-[#FF6B6B] font-semibold text-sm uppercase tracking-wider mb-4">Watch & Play</p>
          <h2 className="text-4xl md:text-5xl font-black text-white mb-4">
            See <span className="gradient-text">GameArena</span> in Action
          </h2>
          <p className="text-gray-400 max-w-xl mx-auto">
            Watch tutorials, tournament highlights, and feature deep dives.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-6 mb-16">
          {videos.map((video, i) => (
            <a key={i} href={video.link} target="_blank" rel="noopener noreferrer"
              className="group block bg-[#1a1a2e] border border-white/5 rounded-2xl overflow-hidden transition-all duration-300 hover:-translate-y-1 card-glow">
              <div className="relative aspect-video bg-gradient-to-br from-[#6C5CE7]/20 to-[#00D2FF]/10 flex items-center justify-center">
                <div className="absolute inset-0 bg-black/40 group-hover:bg-black/20 transition-colors" />
                <div className="relative z-10 w-16 h-16 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center group-hover:scale-110 transition-transform">
                  <FaPlay className="text-white ml-1" size={20} />
                </div>
                <div className="absolute top-3 right-3 bg-black/60 backdrop-blur-sm px-2 py-1 rounded text-xs text-white font-medium">
                  {video.duration}
                </div>
                <div className="absolute top-3 left-3 bg-[#6C5CE7] px-2 py-1 rounded text-xs text-white font-medium">
                  {video.category}
                </div>
              </div>
              <div className="p-5">
                <h3 className="text-white font-bold text-lg mb-2 group-hover:text-[#6C5CE7] transition-colors">{video.title}</h3>
                <p className="text-gray-400 text-sm leading-relaxed">{video.description}</p>
              </div>
            </a>
          ))}
        </div>

        <div className="bg-[#0d0d14] border border-white/5 rounded-2xl p-8">
          <h3 className="text-2xl font-bold text-white mb-6 text-center">Latest Promotions</h3>
          <div className="grid md:grid-cols-3 gap-4">
            {ads.map((ad, i) => (
              <div key={i} className="bg-[#1a1a2e] border border-white/5 rounded-xl p-5 flex flex-col">
                <div className="w-3 h-3 rounded-full mb-3" style={{ background: ad.color }} />
                <h4 className="text-white font-bold mb-2">{ad.title}</h4>
                <p className="text-gray-400 text-sm flex-1">{ad.description}</p>
                <a href="#download" className="mt-4 inline-flex items-center gap-2 text-sm font-semibold transition-colors" style={{ color: ad.color }}>
                  {ad.cta} <FaExternalLinkAlt size={10} />
                </a>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
