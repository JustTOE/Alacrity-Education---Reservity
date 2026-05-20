// Fixture data for Reservity prototype
window.RESERVITY_DATA = (() => {
  const now = new Date(2026, 4, 2, 14, 12); // May 2 2026 14:12 — for stable demo

  const SPACES = [
    {
      id: 'lab-4b', name: 'Chemistry Lab 4B', type: 'lab',
      building: 'Hawthorn Sciences', floor: 4, room: '4B',
      seats: 12, area: 48, price: 18, currency: '$/hr',
      vibes: ['focus', 'group', 'natural-light', 'lab-only'],
      booked: [9, 10, 13, 14, 17, 18],
      events: [
        { startH: 9, endH: 11, title: 'Organic chem lab section', host: 'Prof. Adesanya' },
        { startH: 13, endH: 15, title: 'Polymer synthesis workshop', host: 'Materials Club' },
        { startH: 17, endH: 19, title: 'Open lab hours', host: 'TA office' },
      ],
      viewing: 3, todayBookings: 4,
      blurb: 'Fume hoods, safety gear, precision instruments.',
      description: 'A 48 m² wet lab on the 4th floor of Hawthorn Sciences, certified for organic and inorganic synthesis. Six fume hoods, two analytical balances, a centrifuge, and a small instrument bay (UV-vis, IR, GC). Required: lab certification + closed-toe shoes.',
      amenities: ['6 fume hoods', '2 analytical balances', 'Centrifuge', 'UV-vis spectrometer', 'IR + GC', 'Eyewash station', 'Whiteboard wall', 'Wi-Fi'],
      rules: ['Lab cert required', 'Closed-toe shoes', 'Goggles provided', 'No food'],
      pin: { x: 0.62, y: 0.41 },
    },
    {
      id: 'pod-3', name: 'Coding Pod 3', type: 'pod',
      building: 'Linden Hall', floor: 2, room: '2-08',
      seats: 1, area: 6, price: 4, currency: '$/hr',
      vibes: ['focus', 'solo', 'after-hours'],
      booked: [10, 11, 12, 15, 16],
      viewing: 1, todayBookings: 9,
      blurb: 'Soundproof, dual monitor, ergonomic chair.',
      events: [
        { startH: 10, endH: 12, title: 'Capstone deep work', host: 'Devon K.' },
        { startH: 15, endH: 17, title: 'Algorithms grind', host: 'Maya R.' },
      ],
      description: "A 6 m² soundproof pod on the 2nd floor of Linden Hall. Dual 27\" monitors, mechanical keyboard (or bring your own), Aeron-style chair, dimmable warm lighting. Lock the door, put on headphones, vanish for three hours.",
      amenities: ["Dual 27\" monitors","Soundproofed","Aeron chair","Dimmable lighting","Standing desk","USB-C dock","Wi-Fi 6"],
      rules: ["1 person max","Max 4-hour booking","Quiet floor — phone calls outside"],
      pin: { x: 0.42, y: 0.55 },
    },
    {
      id: 'atrium', name: 'The Glass Atrium', type: 'open',
      building: 'Pavilion North', floor: 1, room: 'A1',
      seats: 24, area: 120, price: 0, currency: 'free',
      vibes: ['loud', 'group', 'natural-light', 'window'],
      booked: [11, 12, 13, 16],
      viewing: 7, todayBookings: 12,
      blurb: 'South-facing glass, plants, big tables.',
      events: [
        { startH: 11, endH: 13, title: 'Coffee + critique', host: 'Design Society' },
        { startH: 16, endH: 18, title: 'Open-mic poetry, no rules', host: 'Linden Writers Co.' },
      ],
      description: "A 120 m² south-facing atrium with floor-to-ceiling windows, hanging plants, and four large communal tables seating six each. The acoustic floor of Pavilion North — bring a coffee, sit somewhere, talk to a stranger.",
      amenities: ["South-facing glass","4 large tables","Plants everywhere","Standing piano","Cafe adjacent","Power at every seat","Free Wi-Fi"],
      rules: ["Open to all","Loud-talk friendly","No reservations after 6pm"],
      pin: { x: 0.28, y: 0.32 },
    },
    {
      id: 'studio-c', name: 'Print Studio C', type: 'studio',
      building: 'Magnolia Arts', floor: 0, room: 'B-12',
      seats: 6, area: 32, price: 8, currency: '$/hr',
      vibes: ['focus', 'group', 'after-hours'],
      booked: [9, 10, 11, 12, 13, 14, 15, 16, 17],
      viewing: 0, todayBookings: 6,
      blurb: 'Letterpress, screens, drying racks. Messy welcome.',
      events: [
        { startH: 10, endH: 13, title: 'Letterpress intro workshop', host: 'Studio Faculty' },
        { startH: 14, endH: 17, title: 'Risograph open hours', host: 'Print Club' },
      ],
      description: "32 m² print studio in the basement of Magnolia Arts. Vandercook letterpress, two screen-print stations, drying racks, and a Risograph. Aprons provided. Things will get on your hands.",
      amenities: ["Letterpress","Screen print x2","Risograph","Drying racks","Aprons + gloves","Wash station","Type drawer library"],
      rules: ["Studio induction required","Clean as you go","No food near presses"],
      pin: { x: 0.71, y: 0.62 },
    },
    {
      id: 'darkroom', name: 'Photo Darkroom 1', type: 'studio',
      building: 'Magnolia Arts', floor: -1, room: 'D-1',
      seats: 4, area: 18, price: 6, currency: '$/hr',
      vibes: ['focus', 'solo', 'after-hours'],
      booked: [],
      viewing: 0, todayBookings: 0,
      blurb: 'Wet bench, enlargers, red safelights.',
      events: [],
      description: "Wet darkroom for B&W silver gelatin printing. Four enlarger stations, archival wash, red safelights only. Bring your negatives and your patience.",
      amenities: ["4 enlargers","Archival wash","Red safelights","Drying lines","Loupe + grain focus","Chemistry refresh daily"],
      rules: ["Photo cert required","Knock before entering","No phones with screens lit"],
      pin: { x: 0.74, y: 0.66 }, surprise: true,
    },
    {
      id: 'studio-grand', name: 'Recording Studio A', type: 'studio',
      building: 'Linden Hall', floor: 1, room: '1-30',
      seats: 5, area: 28, price: 22, currency: '$/hr',
      vibes: ['focus', 'group', 'after-hours'],
      booked: [14, 15, 16, 19, 20],
      viewing: 2, todayBookings: 3,
      blurb: 'Treated room, condenser mics, MIDI rig.',
      events: [
        { startH: 14, endH: 16, title: 'Vocal session — full band', host: 'The Saplings' },
        { startH: 19, endH: 21, title: 'Friday jam · acoustic only', host: 'Music Society' },
      ],
      description: "Acoustically-treated 28 m² recording room with isolation booth. Neumann + SM7B mics, MOTU interface, MIDI rig, full DAW workstation. Engineer optional.",
      amenities: ["Treated room","Iso booth","Neumann + SM7B","MOTU interface","MIDI rig","Logic + Ableton","Drum kit available"],
      rules: ["Audio cert for solo use","No food or liquids","Engineer fee if requested"],
      pin: { x: 0.45, y: 0.49 },
    },
    {
      id: 'pod-7', name: 'Coding Pod 7', type: 'pod',
      building: 'Linden Hall', floor: 3, room: '3-12',
      seats: 1, area: 5, price: 4, currency: '$/hr',
      vibes: ['focus', 'solo', 'window'],
      booked: [9, 10, 11],
      viewing: 2, todayBookings: 5,
      blurb: 'Window seat, single desk, soft acoustic panels.',
      events: [
        { startH: 9, endH: 12, title: 'Thesis writing', host: 'Priya S.' },
      ],
      description: "Single-occupant pod with a south-facing window seat on the 3rd floor of Linden Hall. Soft acoustic panels, single desk, plant on the sill. Quietest spot we have.",
      amenities: ["Window seat","Acoustic panels","Single desk","Plant","Wi-Fi 6","Power at desk"],
      rules: ["1 person max","Quiet floor","Plant lives, do not move"],
      pin: { x: 0.40, y: 0.46 },
    },
    {
      id: 'crit-room', name: 'Crit Room East', type: 'open',
      building: 'Magnolia Arts', floor: 2, room: '2-04',
      seats: 16, area: 56, price: 0, currency: 'free',
      vibes: ['loud', 'group', 'natural-light'],
      booked: [13, 14, 18, 19],
      viewing: 4, todayBookings: 7,
      blurb: 'White walls, movable easels, projector.',
      events: [
        { startH: 13, endH: 17, title: 'Thesis crit · senior studio', host: 'Magnolia Arts Faculty' },
        { startH: 18, endH: 20, title: 'Sketch crawl warmup', host: 'Drawing Club' },
      ],
      description: "56 m² white-walled crit room with movable easels, a ceiling-mounted projector, and pinnable felt walls along two sides. Designed for show-and-tell.",
      amenities: ["White walls","Movable easels x8","Projector + screen","Pin walls","Track lighting","Bluetooth speaker"],
      rules: ["Open to all","Easels stay in room","Lights off when leaving"],
      pin: { x: 0.69, y: 0.58 },
    },
    {
      id: 'lab-2a', name: 'Bio Lab 2A', type: 'lab',
      building: 'Hawthorn Sciences', floor: 2, room: '2A',
      seats: 10, area: 40, price: 16, currency: '$/hr',
      vibes: ['focus', 'group', 'lab-only'],
      booked: [9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
      viewing: 1, todayBookings: 8, fullyBooked: true,
      blurb: 'Microscopes, centrifuges, biosafety cabinet.',
      events: [
        { startH: 9, endH: 12, title: 'Cell culture · BIO 305', host: 'Prof. Chen' },
        { startH: 13, endH: 17, title: 'Microscopy practical', host: 'TA Joaquin' },
        { startH: 18, endH: 20, title: 'Open lab hours', host: 'TA office' },
      ],
      description: "40 m² biology lab with biosafety cabinet, twelve dissecting microscopes, and a centrifuge bank. BSL-1 work only. Currently fully booked through tomorrow — notify to be next.",
      amenities: ["Biosafety cabinet","12 microscopes","Centrifuge bank","Autoclave access","Cold storage","Wash bay"],
      rules: ["Bio cert required","Lab coat + gloves","No solo after-hours"],
      pin: { x: 0.60, y: 0.44 },
    },
    {
      id: 'rooftop', name: 'Rooftop Greenhouse', type: 'open',
      building: 'Hawthorn Sciences', floor: 5, room: 'R',
      seats: 8, area: 36, price: 4, currency: '$/hr',
      vibes: ['natural-light', 'window', 'group', 'after-hours'],
      booked: [12, 13, 17],
      viewing: 5, todayBookings: 4,
      blurb: 'Tomatoes, herbs, occasional bee. Bring a hat.',
      events: [
        { startH: 12, endH: 14, title: 'Tomato pruning + chat', host: 'Garden Society' },
        { startH: 17, endH: 20, title: 'Build night: Arduino + plants', host: 'Hawthorn Robotics' },
      ],
      description: "36 m² rooftop greenhouse on the top of Hawthorn Sciences. Tomatoes, herbs, edible flowers, occasional bees. Spectacular at golden hour. Bring a hat in summer.",
      amenities: ["Working garden","Hand tools","Watering system","Wi-Fi (weak)","Sunshade","Compost bin"],
      rules: ["No pets","No picking without permission","Close all vents when leaving"],
      pin: { x: 0.58, y: 0.38 },
    },
  ];

  const VIBES = [
    { id: 'focus', label: 'Focus mode', icon: '◐' },
    { id: 'loud', label: 'Loud and proud', icon: '◉' },
    { id: 'window', label: 'Window seat', icon: '▢' },
    { id: 'solo', label: 'Solo', icon: '·' },
    { id: 'group', label: 'Group of 4+', icon: '◊' },
    { id: 'natural-light', label: 'Natural light', icon: '☼' },
    { id: 'after-hours', label: 'After hours', icon: '◑' },
    { id: 'lab-only', label: 'Lab certified', icon: '⌬' },
  ];

  const EVENTS = [
    {
      id: 'ev-poetry', title: 'Open-mic poetry, no rules',
      host: 'Linden Writers Co.', hostInitials: 'LW',
      space: 'The Glass Atrium', building: 'Pavilion North',
      status: 'live', startH: 13, endH: 16,
      attendees: 28, capacity: 40, tag: 'Open to all',
      blurb: 'Bring a notebook or just listen. Bring tea.',
    },
    {
      id: 'ev-thesis', title: 'Thesis crit · senior studio',
      host: 'Magnolia Arts Faculty', hostInitials: 'MA',
      space: 'Crit Room East', building: 'Magnolia Arts',
      status: 'live', startH: 13, endH: 17,
      attendees: 14, capacity: 16, tag: 'Invite only',
      blurb: '4th-year thesis projects. Quiet observers welcome.',
    },
    {
      id: 'ev-build', title: 'Build night: Arduino + plants',
      host: 'Hawthorn Robotics Club', hostInitials: 'HR',
      space: 'Rooftop Greenhouse', building: 'Hawthorn Sciences',
      status: 'soon', startH: 17, endH: 20,
      attendees: 22, capacity: 30, tag: 'Free · drop-in',
      blurb: 'We\'re wiring soil sensors. Solder at your own risk.',
    },
    {
      id: 'ev-jam', title: 'Friday jam · acoustic only',
      host: 'Music Society', hostInitials: 'MS',
      space: 'Recording Studio A', building: 'Linden Hall',
      status: 'soon', startH: 19, endH: 22,
      attendees: 9, capacity: 12, tag: 'Bring an instrument',
      blurb: 'Soft chairs, soft lights, soft songs.',
    },
    {
      id: 'ev-talk', title: 'Mira Adesanya · sci-comm talk',
      host: 'Hawthorn Sciences', hostInitials: 'HS',
      space: 'Auditorium 1', building: 'Hawthorn Sciences',
      status: 'tomorrow', startH: 11, endH: 12,
      attendees: 84, capacity: 220, tag: 'Free · RSVP',
      blurb: 'How to write about science without lying or boring anyone.',
    },
  ];

  const HISTORY = [
    { date: 'Apr 28', space: 'Coding Pod 3', dur: '2h', tag: 'Solo focus' },
    { date: 'Apr 24', space: 'Chemistry Lab 4B', dur: '3h', tag: 'Group session' },
    { date: 'Apr 22', space: 'Coding Pod 3', dur: '4h', tag: 'Solo focus' },
    { date: 'Apr 19', space: 'The Glass Atrium', dur: '1h', tag: 'Drop-in' },
    { date: 'Apr 16', space: 'Recording Studio A', dur: '2h', tag: 'Group session' },
    { date: 'Apr 14', space: 'Coding Pod 3', dur: '3h', tag: 'Solo focus' },
    { date: 'Apr 11', space: 'Print Studio C', dur: '2h', tag: 'Group session' },
  ];

  // Deterministic per-date availability for any space.
  // We use the canonical "today" booked array as a seed and shuffle/perturb by date.
  function hashStr(s) {
    let h = 2166136261 >>> 0;
    for (let i = 0; i < s.length; i++) { h ^= s.charCodeAt(i); h = Math.imul(h, 16777619); }
    return h >>> 0;
  }
  function dateKey(d) {
    return `${d.getFullYear()}-${d.getMonth()+1}-${d.getDate()}`;
  }
  function bookedForDate(space, d) {
    const today = now;
    const sameDay = d.getFullYear()===today.getFullYear() && d.getMonth()===today.getMonth() && d.getDate()===today.getDate();
    if (sameDay) return { booked: space.booked.slice(), events: (space.events || []).slice(), fullyBooked: !!space.fullyBooked };

    // Generate stable but varied schedule
    const seed = hashStr(space.id + '|' + dateKey(d));
    let s = seed;
    const rand = () => { s = (s * 1664525 + 1013904223) >>> 0; return s / 0x100000000; };

    const HOURS = Array.from({ length: 14 }, (_, i) => 8 + i); // 8..21
    // base density correlates with the space's "popularity" (todayBookings)
    const density = Math.min(0.85, 0.15 + (space.todayBookings || 4) * 0.06);
    const booked = [];
    const events = [];
    // generate 0..3 event blocks
    const evCount = Math.floor(rand() * 3) + (density > 0.5 ? 1 : 0);
    const titles = [
      ['Open hours', 'Drop-in', 'Workshop', 'Group session', 'Office hours', 'Build night', 'Critique', 'Practice', 'Open lab'],
    ][0];
    const hosts = ['TA office', 'Studio Faculty', 'Maya R.', 'Devon K.', 'Priya S.', 'Music Society', 'Design Society'];
    let cursor = 8 + Math.floor(rand() * 3);
    for (let e = 0; e < evCount && cursor < 20; e++) {
      const len = 1 + Math.floor(rand() * 3);
      const startH = cursor;
      const endH = Math.min(22, startH + len);
      events.push({ startH, endH, title: titles[Math.floor(rand() * titles.length)], host: hosts[Math.floor(rand() * hosts.length)] });
      for (let h = startH; h < endH; h++) booked.push(h);
      cursor = endH + 1 + Math.floor(rand() * 3);
    }
    // sprinkle stray bookings
    HOURS.forEach(h => { if (!booked.includes(h) && rand() < density * 0.35) booked.push(h); });
    booked.sort((a,b)=>a-b);
    const fullyBooked = booked.length >= HOURS.length - 1;
    return { booked, events, fullyBooked };
  }

  // Build a 14-day window starting "today"
  const DATE_WINDOW = Array.from({ length: 14 }, (_, i) => {
    const d = new Date(now);
    d.setDate(d.getDate() + i);
    d.setHours(0, 0, 0, 0);
    return d;
  });

  return { now, SPACES, VIBES, EVENTS, HISTORY, bookedForDate, dateKey, DATE_WINDOW };
})();
