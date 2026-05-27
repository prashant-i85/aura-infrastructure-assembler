import os

with open('src/main/resources/static/index.html', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('''    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Inter', sans-serif;
      background: var(--bg-base);
      color: var(--text-primary);
      min-height: 100vh;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 1rem;
      background-image: radial-gradient(circle at top right, rgba(0,122,255,0.05), transparent 40%),
                        radial-gradient(circle at bottom left, rgba(52,199,89,0.05), transparent 40%);
    }''', '''    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Inter', sans-serif;
      background: linear-gradient(135deg, #fdfbf5 0%, #f4e8c1 100%);
      color: var(--text-primary);
      min-height: 100vh;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 1rem;
    }''')

text = text.replace('''.btn-primary { background: var(--accent); color: #fff; }
    .btn-primary:hover:not(:disabled) { background: #006ce6; transform: translateY(-1px); }''', '''.btn-primary { background: #222; color: #fff; border: 1px solid transparent; }
    .btn-primary:hover:not(:disabled) { background: #333; border-color: rgba(244, 232, 193, 0.5); color: #fff2cc; transform: translateY(-1px); }''')

style_end = "</style>"
profile_css = '''
    .profile-dropdown-container { position: relative; display: inline-block; }
    .profile-dropdown {
      position: absolute; right: 0; top: 120%;
      background: var(--bg-card); border: 1px solid var(--border);
      border-radius: 12px; padding: 12px; display: none;
      flex-direction: column; align-items: center; gap: 8px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.1); width: max-content;
      z-index: 100;
    }
    .profile-dropdown-container:hover .profile-dropdown { display: flex; }
    .profile-dropdown-img { width: 40px; height: 40px; border-radius: 50%; }

    /* Fix dark mode theme vars for profile dropdown to ensure readable text */
    [data-theme="dark"] .profile-dropdown { border-color: rgba(255,255,255,0.1); }
</style>'''
text = text.replace(style_end, profile_css)

text = text.replace("const tabs = ['Dashboard', 'People', 'Hiring', 'Devices'];", "const tabs = ['Dashboard', 'Track'];")

topbar_btns = '''<button className="icon-btn">??</button>
                 <button className="icon-btn">??</button>'''
topbar_btns_new = '''<button className="icon-btn">??</button>
                 <div className="profile-dropdown-container">
                   <button className="icon-btn">??</button>
                   <div className="profile-dropdown">
                     <img src="https://ui-avatars.com/api/?name=Oyasumiii&background=random" alt="Avatar" className="profile-dropdown-img"/>
                     <span style={{fontSize: '0.85rem', fontWeight: 600}}>Welcome, Oyasumiii</span>
                   </div>
                 </div>'''
text = text.replace(topbar_btns, topbar_btns_new)

bottom_row = '''          {/* Bottom Row */}
          <div className="bottom-row">
             <div className="bottom-track-card">
                <div style={{fontSize:'1.2rem'}}>??</div>
                <div style={{flex: 1}}>
                  <div style={{fontWeight:600}}>Track existing request</div>
                  <div className="text-secondary" style={{fontSize:'0.85rem'}}>Paste a Request ID to monitor live status</div>
                </div>
                <input className="form-control" style={{width: '300px'}} placeholder="Request ID..." value={manualId} onChange={e=>setManualId(e.target.value)} />
                <button type="button" className="btn btn-ghost" style={{width: 'auto'}} onClick={() => setTrackingId(manualId)} disabled={!manualId}>Track</button>
             </div>
          </div>'''
text = text.replace(bottom_row, "")

track_sect = '''    function TrackSection() {
      const [manualId, setManualId] = useState('');
      const [trackingId, setTrackingId] = useState(null);
      return (
        <div style={{maxWidth: '800px', margin: '0 auto'}}>
          <div className="bottom-track-card" style={{marginBottom: '1.5rem'}}>
             <div style={{fontSize:'1.2rem'}}>??</div>
             <div style={{flex: 1}}>
               <div style={{fontWeight:600}}>Track existing request</div>
               <div className="text-secondary" style={{fontSize:'0.85rem'}}>Paste a Request ID to monitor live status</div>
             </div>
             <input className="form-control" style={{width: '300px'}} placeholder="Request ID..." value={manualId} onChange={e=>setManualId(e.target.value)} />
             <button type="button" className="btn btn-ghost" style={{width: 'auto'}} onClick={() => setTrackingId(manualId)} disabled={!manualId}>Track</button>
          </div>
          {trackingId && (
            <div className="card">
               <div className="card-title"><span className="icon">??</span> Live Status</div>
               <StatusTracker requestId={trackingId} />
            </div>
          )}
        </div>
      );
    }

    function App() {'''
text = text.replace("    function App() {", track_sect)

main_render_old = '''             {activeTab === 'Dashboard' ? <ProvisionDashboard /> : (
                <div style={{textAlign:'center', marginTop:'4rem', color:'var(--text-secondary)'}}>
                  <h2>{activeTab} Module</h2>
                  <p>In development.</p>
                </div>
             )}'''
main_render_new = '''             {activeTab === 'Dashboard' && <ProvisionDashboard />}
             {activeTab === 'Track' && <TrackSection />}
             {activeTab !== 'Dashboard' && activeTab !== 'Track' && (
                <div style={{textAlign:'center', marginTop:'4rem', color:'var(--text-secondary)'}}>
                  <h2>{activeTab} Module</h2>
                  <p>In development.</p>
                </div>
             )}'''
text = text.replace(main_render_old, main_render_new)

resource_type_old = '''<option value="COMPUTE">Compute</option><option value="STORAGE">Storage</option>'''
resource_type_new = '''<option value="COMPUTE">Compute</option><option value="STORAGE">Storage</option><option value="AI_WORKSPACE">AI Workspace</option>'''
text = text.replace(resource_type_old, resource_type_new)

ami_card_old = '''          {/* Mid Column */}
          <div className="col-mid">
            <div className="card">
               <div className="card-title"><span className="icon">??</span> Image & Identity</div>
               
               <div className="ami-logo-block">
                 <div className="ami-icon-box">{form.provider === 'AWS' ? '??' : '??'}</div>
                 <div>
                   <div style={{fontWeight:600}}>{form.provider === 'AWS' ? 'Ubuntu 22.04 LTS' : 'Debian 11'}</div>
                   <div className="text-secondary" style={{fontSize:'0.8rem'}}>{form.provider === 'AWS' ? 'Provided by AWS Marketplace' : 'Provided by Google Cloud'}</div>
                 </div>
               </div>

               {form.resourceType === 'COMPUTE' && ('''
ami_card_new = '''          {/* Mid Column */}
          <div className="col-mid">
            <div className="card">
               <div className="card-title">
                 <span className="icon">??</span> 
                 {form.resourceType === 'STORAGE' ? 'Storage Details' : 'Image & Identity'}
               </div>
               
               {form.resourceType !== 'STORAGE' && (
                 <div className="ami-logo-block">
                   <div className="ami-icon-box">{form.provider === 'AWS' ? '??' : '??'}</div>
                   <div>
                     <div style={{fontWeight:600}}>
                       {form.provider === 'AWS' 
                         ? (form.resourceType === 'AI_WORKSPACE' ? 'AI Deep Learning AMI' : 'Ubuntu 22.04 LTS') 
                         : 'Debian 11'}
                     </div>
                     <div className="text-secondary" style={{fontSize:'0.8rem', display:'flex', alignItems:'center', gap:'4px'}}>
                       {form.provider === 'AWS' ? <strong style={{color:'#ff9900'}}>[AWS]</strong> : <strong style={{color:'#4285F4'}}>[GCP]</strong>}
                       {form.provider === 'AWS' ? ' Provided by AWS Marketplace' : ' Provided by Google Cloud'}
                     </div>
                   </div>
                 </div>
               )}

               {(form.resourceType === 'COMPUTE' || form.resourceType === 'AI_WORKSPACE') && ('''
text = text.replace(ami_card_old, ami_card_new)

text = text.replace("{form.resourceType === 'COMPUTE' && (\\n                <div className=\\\"form-group\\\">\\n                  <label>Instance Type</label>", 
"{(form.resourceType === 'COMPUTE' || form.resourceType === 'AI_WORKSPACE') && (\\n                <div className=\\\"form-group\\\">\\n                  <label>Instance Type</label>")

with open('src/main/resources/static/index.html', 'w', encoding='utf-8') as f:
    f.write(text)
