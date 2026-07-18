import React, { useState, useEffect } from 'react'
import { FiDownload, FiSmartphone, FiCheckCircle } from 'react-icons/fi'
import { FaGooglePlay, FaApple } from 'react-icons/fa'

const GITHUB_REPO = 'ALV-m/GameArena'

const requirements = [
  'Android 8.0 (API 26) or higher',
  '2 GB RAM minimum (4 GB recommended)',
  '500 MB free storage',
  'Internet connection for multiplayer',
]

export default function DownloadSection() {
  const [latestRelease, setLatestRelease] = useState(null)

  useEffect(() => {
    fetch(`https://api.github.com/repos/${GITHUB_REPO}/releases/latest`)
      .then(res => res.json())
      .then(data => {
        if (data.tag_name) {
          const apk = data.assets?.find(a => a.name.endsWith('.apk'))
          setLatestRelease({
            version: data.tag_name,
            date: new Date(data.published_at).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
            apkUrl: apk?.browser_download_url || null,
            apkSize: apk ? (apk.size / 1024 / 1024).toFixed(1) + ' MB' : null,
            body: data.body,
          })
        }
      })
      .catch(() => {})
  }, [])

  return (
    <section id="download" className="py-24 px-6 bg-[#0d0d14]">
      <div className="max-w-5xl mx-auto">
        <div className="text-center mb-16">
          <p className="text-[#22C55E] font-semibold text-sm uppercase tracking-wider mb-4">Get the App</p>
          <h2 className="text-4xl md:text-5xl font-black text-white mb-4">
            Download <span className="gradient-text">GameArena</span>
          </h2>
          <p className="text-gray-400 max-w-xl mx-auto">
            Free to download. Free to play. Win real money.
          </p>
        </div>

        <div className="grid md:grid-cols-2 gap-8 items-center">
          <div className="bg-[#1a1a2e] border border-white/5 rounded-2xl p-8">
            <h3 className="text-2xl font-bold text-white mb-6">Download APK</h3>

            {latestRelease ? (
              <div className="space-y-4">
                <div className="flex items-center gap-3 text-sm text-gray-400">
                  <span className="text-[#6C5CE7] font-bold">Version {latestRelease.version}</span>
                  <span>•</span>
                  <span>{latestRelease.date}</span>
                  {latestRelease.apkSize && (
                    <>
                      <span>•</span>
                      <span>{latestRelease.apkSize}</span>
                    </>
                  )}
                </div>

                {latestRelease.apkUrl ? (
                  <a href={latestRelease.apkUrl} download
                    className="download-btn flex items-center justify-center gap-3 w-full py-4 rounded-xl text-white font-bold text-lg transition-all">
                    <FiDownload size={22} />
                    Download APK
                  </a>
                ) : (
                  <a href={`https://github.com/${GITHUB_REPO}/releases`}
                    target="_blank" rel="noopener noreferrer"
                    className="download-btn flex items-center justify-center gap-3 w-full py-4 rounded-xl text-white font-bold text-lg transition-all">
                    <FiDownload size={22} />
                    View on GitHub
                  </a>
                )}

                {latestRelease.body && (
                  <p className="text-xs text-gray-500 leading-relaxed mt-2">{latestRelease.body.substring(0, 200)}...</p>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                <a href={`https://github.com/${GITHUB_REPO}/releases`}
                  target="_blank" rel="noopener noreferrer"
                  className="download-btn flex items-center justify-center gap-3 w-full py-4 rounded-xl text-white font-bold text-lg transition-all">
                  <FiDownload size={22} />
                  Download Latest Release
                </a>
                <p className="text-xs text-gray-500 text-center">Fetching latest version from GitHub...</p>
              </div>
            )}

            <div className="mt-6 pt-6 border-t border-white/5">
              <p className="text-xs text-gray-500 mb-3">Or install from:</p>
              <div className="flex gap-3">
                <a href="https://play.google.com/store/apps/details?id=com.gamearena.booster"
                  target="_blank" rel="noopener noreferrer"
                  className="flex items-center gap-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-lg px-4 py-2.5 text-sm text-white transition-colors">
                  <FaGooglePlay size={16} /> Google Play
                </a>
                <div className="flex items-center gap-2 bg-white/5 border border-white/10 rounded-lg px-4 py-2.5 text-sm text-gray-500 cursor-not-allowed">
                  <FaApple size={16} /> Coming Soon
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-6">
            <div>
              <h4 className="text-white font-bold text-lg mb-4 flex items-center gap-2">
                <FiSmartphone className="text-[#6C5CE7]" size={20} />
                System Requirements
              </h4>
              <div className="space-y-3">
                {requirements.map((req, i) => (
                  <div key={i} className="flex items-center gap-3">
                    <FiCheckCircle className="text-[#22C55E] flex-shrink-0" size={16} />
                    <span className="text-gray-400 text-sm">{req}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-[#1a1a2e] border border-white/5 rounded-xl p-5">
              <h4 className="text-white font-bold mb-3">Installation Guide</h4>
              <ol className="space-y-2 text-sm text-gray-400">
                <li className="flex gap-3">
                  <span className="text-[#6C5CE7] font-bold flex-shrink-0">1.</span>
                  Download the APK file from above
                </li>
                <li className="flex gap-3">
                  <span className="text-[#6C5CE7] font-bold flex-shrink-0">2.</span>
                  Open the APK and allow "Install from unknown sources"
                </li>
                <li className="flex gap-3">
                  <span className="text-[#6C5CE7] font-bold flex-shrink-0">3.</span>
                  Follow the installation prompts
                </li>
                <li className="flex gap-3">
                  <span className="text-[#6C5CE7] font-bold flex-shrink-0">4.</span>
                  Sign in with your phone number or email
                </li>
              </ol>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
