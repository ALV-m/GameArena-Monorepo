import React from 'react'
import Navbar from './components/Navbar'
import Hero from './components/Hero'
import Features from './components/Features'
import Leaderboard from './components/Leaderboard'
import GamingVideos from './components/GamingVideos'
import DownloadSection from './components/DownloadSection'
import Footer from './components/Footer'

export default function App() {
  return (
    <div className="min-h-screen bg-[#0a0a0f]">
      <Navbar />
      <Hero />
      <Features />
      <Leaderboard />
      <GamingVideos />
      <DownloadSection />
      <Footer />
    </div>
  )
}
