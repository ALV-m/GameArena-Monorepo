import React from 'react'
import { FaBolt, FaTrophy, FaWallet, FaLayerGroup, FaShieldAlt, FaChartLine, FaGamepad, FaUsers } from 'react-icons/fa'

const features = [
  {
    icon: FaBolt,
    title: 'Game Booster',
    description: 'Suspend background apps, clear RAM, and optimize CPU for maximum FPS. One tap to peak performance.',
    color: '#6C5CE7',
  },
  {
    icon: FaLayerGroup,
    title: 'In-Game Overlay',
    description: 'Real-time FPS, CPU, RAM, network stats floating over your game. 8 panels: performance, chat, wallet, and more.',
    color: '#00D2FF',
  },
  {
    icon: FaTrophy,
    title: 'Tournaments',
    description: 'Create or join tournaments. Single elimination, round robin, best of 5. Entry fees start from KES 50.',
    color: '#FFD700',
  },
  {
    icon: FaGamepad,
    title: '1v1 Challenges',
    description: 'Challenge any player head-to-head. Set stakes, play the match, submit results, winner takes the pot.',
    color: '#FF6B6B',
  },
  {
    icon: FaWallet,
    title: 'Real Money Wallet',
    description: 'Deposit via M-Pesa, Stripe, or PayPal. Withdraw winnings instantly. Full transaction history.',
    color: '#22C55E',
  },
  {
    icon: FaShieldAlt,
    title: 'Result Verification',
    description: 'Submit screenshots as proof. OCR-powered score detection. Dispute system with admin resolution.',
    color: '#F59E0B',
  },
  {
    icon: FaChartLine,
    title: 'Thermal Diagnostics',
    description: 'Find what causes frame drops. CPU/GPU/Skin temperature correlation graphs. Session recording.',
    color: '#EF4444',
  },
  {
    icon: FaUsers,
    title: 'Leaderboard',
    description: 'Climb the ranks. ELO-based matchmaking. Track wins, losses, and global standing.',
    color: '#A78BFA',
  },
]

export default function Features() {
  return (
    <section id="features" className="py-24 px-6">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <p className="text-[#6C5CE7] font-semibold text-sm uppercase tracking-wider mb-4">Features</p>
          <h2 className="text-4xl md:text-5xl font-black text-white mb-4">
            Everything You Need to <span className="gradient-text">Dominate</span>
          </h2>
          <p className="text-gray-400 max-w-xl mx-auto">
            From boosting your phone's performance to competing for real cash prizes — GameArena has it all.
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((feature, i) => (
            <div key={i} className="card-glow bg-[#1a1a2e] border border-white/5 rounded-2xl p-6 transition-all duration-300 hover:-translate-y-1">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center mb-4" style={{ background: feature.color + '15' }}>
                <feature.icon size={24} style={{ color: feature.color }} />
              </div>
              <h3 className="text-white font-bold text-lg mb-2">{feature.title}</h3>
              <p className="text-gray-400 text-sm leading-relaxed">{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
