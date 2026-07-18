import React, { useState, useEffect } from 'react'
import { FaCrown, FaMedal } from 'react-icons/fa'

const API_URL = import.meta.env.VITE_API_URL || 'https://gamearena-api.onrender.com'

const fallbackLeaderboard = [
  { rank: 1, username: 'ProGamer_KE', elo_rating: 1850, total_wins: 47, total_losses: 8 },
  { rank: 2, username: 'ShadowStrike', elo_rating: 1780, total_wins: 42, total_losses: 12 },
  { rank: 3, username: 'NightHawk', elo_rating: 1720, total_wins: 38, total_losses: 10 },
  { rank: 4, username: 'BlazeFury', elo_rating: 1690, total_wins: 35, total_losses: 14 },
  { rank: 5, username: 'TitanX', elo_rating: 1650, total_wins: 33, total_losses: 11 },
  { rank: 6, username: 'CyberWolf', elo_rating: 1620, total_wins: 30, total_losses: 15 },
  { rank: 7, username: 'PhantomACE', elo_rating: 1590, total_wins: 28, total_losses: 9 },
  { rank: 8, username: 'StormBreaker', elo_rating: 1560, total_wins: 26, total_losses: 13 },
]

const rankColors = ['#FFD700', '#C0C0C0', '#CD7F32']
const rankIcons = [FaCrown, FaMedal, FaMedal]

export default function Leaderboard() {
  const [players, setPlayers] = useState(fallbackLeaderboard)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch(`${API_URL}/api/users/leaderboard?limit=8`)
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data) && data.length > 0) {
          setPlayers(data.map((p, i) => ({ ...p, rank: i + 1 })))
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  return (
    <section id="leaderboard" className="py-24 px-6 bg-[#0d0d14]">
      <div className="max-w-4xl mx-auto">
        <div className="text-center mb-16">
          <p className="text-[#FFD700] font-semibold text-sm uppercase tracking-wider mb-4">Leaderboard</p>
          <h2 className="text-4xl md:text-5xl font-black text-white mb-4">
            Top <span className="gradient-text">Players</span>
          </h2>
          <p className="text-gray-400 max-w-xl mx-auto">
            The best competitors ranked by ELO rating. Can you make it to the top?
          </p>
        </div>

        <div className="bg-[#1a1a2e] border border-white/5 rounded-2xl overflow-hidden">
          <div className="grid grid-cols-12 gap-4 px-6 py-3 border-b border-white/5 text-xs text-gray-500 uppercase tracking-wider font-semibold">
            <div className="col-span-1">#</div>
            <div className="col-span-5">Player</div>
            <div className="col-span-2 text-right">ELO</div>
            <div className="col-span-2 text-center">W / L</div>
            <div className="col-span-2 text-right">Win Rate</div>
          </div>

          {players.map((player, i) => {
            const winRate = player.total_wins + player.total_losses > 0
              ? ((player.total_wins / (player.total_wins + player.total_losses)) * 100).toFixed(1)
              : '0.0'

            return (
              <div key={i} className={`grid grid-cols-12 gap-4 px-6 py-4 items-center transition-colors hover:bg-white/[0.02] ${i < players.length - 1 ? 'border-b border-white/[0.03]' : ''}`}>
                <div className="col-span-1">
                  {i < 3 ? (
                    <div className="w-8 h-8 rounded-full flex items-center justify-center" style={{ background: rankColors[i] + '20' }}>
                      {React.createElement(rankIcons[i], { size: 14, style: { color: rankColors[i] } })}
                    </div>
                  ) : (
                    <span className="text-gray-500 font-medium text-sm ml-2">{player.rank || i + 1}</span>
                  )}
                </div>
                <div className="col-span-5">
                  <p className="text-white font-semibold text-sm">{player.username}</p>
                </div>
                <div className="col-span-2 text-right">
                  <span className="text-[#6C5CE7] font-bold text-sm">{player.elo_rating}</span>
                </div>
                <div className="col-span-2 text-center">
                  <span className="text-green-400 text-sm">{player.total_wins || 0}</span>
                  <span className="text-gray-600 mx-1">/</span>
                  <span className="text-red-400 text-sm">{player.total_losses || 0}</span>
                </div>
                <div className="col-span-2 text-right">
                  <span className={`text-sm font-medium ${parseFloat(winRate) >= 60 ? 'text-green-400' : parseFloat(winRate) >= 40 ? 'text-yellow-400' : 'text-red-400'}`}>
                    {winRate}%
                  </span>
                </div>
              </div>
            )
          })}
        </div>

        <div className="text-center mt-8">
          <a href="#download" className="inline-flex items-center gap-2 text-[#6C5CE7] hover:text-[#00D2FF] transition-colors font-semibold text-sm">
            Join the competition — Download GameArena →
          </a>
        </div>
      </div>
    </section>
  )
}
