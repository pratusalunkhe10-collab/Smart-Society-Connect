const STORAGE_KEY = 'ssc_mock_db_v1';

const now = new Date();
const isoDaysFromNow = (days, hour = 10) => {
  const date = new Date(now);
  date.setDate(date.getDate() + days);
  date.setHours(hour, 0, 0, 0);
  return date.toISOString();
};

const seed = {
  users: [
    { id: 1, name: 'Aarav Kulkarni', email: 'admin@society.com', mobile: '9876500001', password: 'Admin@123', role: 'ADMIN', flat: 'Office' },
    { id: 2, name: 'Rushikesh Dhande', email: 'resident@society.com', mobile: '9876500002', password: 'Resident@123', role: 'RESIDENT', flat: 'A-304' },
    { id: 3, name: 'Suresh Patil', email: 'security@society.com', mobile: '9876500003', password: 'Security@123', role: 'SECURITY', flat: 'Gate 1' },
    { id: 4, name: 'Neha Shah', email: 'accountant@society.com', mobile: '9876500004', password: 'Accountant@123', role: 'ACCOUNTANT', flat: 'Office' },
    { id: 5, name: 'Meera Joshi', email: 'committee@society.com', mobile: '9876500005', password: 'Committee@123', role: 'SECRETARY', flat: 'B-201' },
  ],
  residents: [
    { id: 101, name: 'Rushikesh Dhande', email: 'resident@society.com', mobile: '9876500002', wing: 'A', flatNo: '304', memberType: 'Owner', familyMembers: 4, status: 'ACTIVE', moveInDate: '2025-02-10' },
    { id: 102, name: 'Ananya Deshmukh', email: 'ananya@example.com', mobile: '9876511223', wing: 'A', flatNo: '101', memberType: 'Owner', familyMembers: 3, status: 'ACTIVE', moveInDate: '2024-11-18' },
    { id: 103, name: 'Rohan Mehta', email: 'rohan@example.com', mobile: '9876522334', wing: 'B', flatNo: '205', memberType: 'Tenant', familyMembers: 2, status: 'ACTIVE', moveInDate: '2026-01-05' },
    { id: 104, name: 'Priya Nair', email: 'priya@example.com', mobile: '9876533445', wing: 'C', flatNo: '402', memberType: 'Owner', familyMembers: 5, status: 'ACTIVE', moveInDate: '2023-08-22' },
    { id: 105, name: 'Vikram Singh', email: 'vikram@example.com', mobile: '9876544556', wing: 'B', flatNo: '110', memberType: 'Tenant', familyMembers: 3, status: 'INACTIVE', moveInDate: '2025-06-01' },
  ],
  visitors: [
    { id: 201, name: 'Kunal More', mobile: '9822011111', purpose: 'Family visit', residentName: 'Rushikesh Dhande', flat: 'A-304', vehicleNo: 'MH 15 AB 4411', status: 'APPROVED', checkIn: isoDaysFromNow(0, 11), checkOut: null, passCode: 'SSC-V201' },
    { id: 202, name: 'Amazon Delivery', mobile: '9000012211', purpose: 'Delivery', residentName: 'Ananya Deshmukh', flat: 'A-101', vehicleNo: '', status: 'PENDING', checkIn: null, checkOut: null, passCode: 'SSC-V202' },
    { id: 203, name: 'Sanjay Electrician', mobile: '9822099911', purpose: 'Maintenance', residentName: 'Priya Nair', flat: 'C-402', vehicleNo: 'MH 12 KL 8891', status: 'CHECKED_OUT', checkIn: isoDaysFromNow(-1, 9), checkOut: isoDaysFromNow(-1, 12), passCode: 'SSC-V203' },
    { id: 204, name: 'Swiggy Delivery', mobile: '9000099999', purpose: 'Food delivery', residentName: 'Rohan Mehta', flat: 'B-205', vehicleNo: '', status: 'REJECTED', checkIn: null, checkOut: null, passCode: 'SSC-V204' },
  ],
  complaints: [
    { id: 301, title: 'Lift not working', category: 'Electrical', description: 'Lift in A wing stops at the second floor.', raisedBy: 'Rushikesh Dhande', flat: 'A-304', priority: 'HIGH', status: 'IN_PROGRESS', createdAt: isoDaysFromNow(-2, 9), assignedTo: 'Maintenance Team' },
    { id: 302, title: 'Water leakage in parking', category: 'Plumbing', description: 'Continuous leakage near parking slot B-12.', raisedBy: 'Meera Joshi', flat: 'B-201', priority: 'MEDIUM', status: 'OPEN', createdAt: isoDaysFromNow(-1, 14), assignedTo: 'Unassigned' },
    { id: 303, title: 'Street light issue', category: 'Electrical', description: 'Two lights are not working near garden.', raisedBy: 'Priya Nair', flat: 'C-402', priority: 'LOW', status: 'RESOLVED', createdAt: isoDaysFromNow(-8, 10), assignedTo: 'Electrical Vendor' },
    { id: 304, title: 'Noise after quiet hours', category: 'Security', description: 'Loud music after 11 PM in B wing.', raisedBy: 'Ananya Deshmukh', flat: 'A-101', priority: 'MEDIUM', status: 'OPEN', createdAt: isoDaysFromNow(-3, 23), assignedTo: 'Security Team' },
  ],
  bills: [
    { id: 401, billNo: 'BILL-2026-071', residentId: 101, residentName: 'Rushikesh Dhande', flat: 'A-304', month: 'July 2026', billingMonth: now.getMonth() + 1, billingYear: now.getFullYear(), maintenance: 2800, water: 350, parking: 500, penalty: 0, total: 3650, dueDate: isoDaysFromNow(8), status: 'PENDING', paidAt: null, paymentMode: null },
    { id: 402, billNo: 'BILL-2026-072', residentId: 102, residentName: 'Ananya Deshmukh', flat: 'A-101', month: 'July 2026', billingMonth: now.getMonth() + 1, billingYear: now.getFullYear(), maintenance: 2800, water: 300, parking: 0, penalty: 0, total: 3100, dueDate: isoDaysFromNow(8), status: 'PAID', paidAt: isoDaysFromNow(-2, 16), paymentMode: 'UPI' },
    { id: 403, billNo: 'BILL-2026-073', residentId: 103, residentName: 'Rohan Mehta', flat: 'B-205', month: 'July 2026', billingMonth: now.getMonth() + 1, billingYear: now.getFullYear(), maintenance: 2800, water: 320, parking: 500, penalty: 200, total: 3820, dueDate: isoDaysFromNow(-3), status: 'OVERDUE', paidAt: null, paymentMode: null },
    { id: 404, billNo: 'BILL-2026-074', residentId: 104, residentName: 'Priya Nair', flat: 'C-402', month: 'July 2026', billingMonth: now.getMonth() + 1, billingYear: now.getFullYear(), maintenance: 2800, water: 390, parking: 500, penalty: 0, total: 3690, dueDate: isoDaysFromNow(8), status: 'PAID', paidAt: isoDaysFromNow(-5, 13), paymentMode: 'NET_BANKING' },
    { id: 405, billNo: 'BILL-2026-075', residentId: 105, residentName: 'Meera Joshi', flat: 'B-201', month: 'July 2026', billingMonth: now.getMonth() + 1, billingYear: now.getFullYear(), maintenance: 2800, water: 310, parking: 0, penalty: 0, total: 3110, dueDate: isoDaysFromNow(8), status: 'PENDING', paidAt: null, paymentMode: null },
  ],
  payments: [
    { paymentId: 601, billingId: 402, amountPaid: 3100, paymentMode: 'UPI', transactionReference: 'UPI-SSC-202607-102', paymentStatus: 'SUCCESS', paymentDate: isoDaysFromNow(-2, 16) },
    { paymentId: 602, billingId: 404, amountPaid: 3690, paymentMode: 'NET_BANKING', transactionReference: 'NB-SSC-202607-104', paymentStatus: 'SUCCESS', paymentDate: isoDaysFromNow(-5, 13) },
  ],
  meetings: [
    { id: 501, title: 'Monthly Committee Meeting', type: 'Committee', date: isoDaysFromNow(3, 19), venue: 'Club House', agenda: 'Maintenance collection, security vendor review and monsoon preparation.', status: 'UPCOMING', attendees: 8, createdBy: 'Meera Joshi' },
    { id: 502, title: 'Annual General Meeting', type: 'General Body', date: isoDaysFromNow(16, 11), venue: 'Community Hall', agenda: 'Budget approval, committee election and annual report.', status: 'UPCOMING', attendees: 0, createdBy: 'Aarav Kulkarni' },
    { id: 503, title: 'Emergency Water Supply Review', type: 'Emergency', date: isoDaysFromNow(-4, 18), venue: 'Society Office', agenda: 'Review tanker supply and pump repairs.', status: 'COMPLETED', attendees: 12, createdBy: 'Aarav Kulkarni' },
  ],
  activities: [
    { id: 1, type: 'visitor', title: 'Visitor approved for A-304', detail: 'Kunal More received a QR pass.', time: isoDaysFromNow(0, 10) },
    { id: 2, type: 'payment', title: 'Maintenance payment received', detail: 'A-101 paid ₹3,100 using UPI.', time: isoDaysFromNow(-1, 15) },
    { id: 3, type: 'complaint', title: 'New complaint raised', detail: 'Water leakage reported in parking.', time: isoDaysFromNow(-1, 14) },
    { id: 4, type: 'meeting', title: 'Meeting notice published', detail: 'Monthly Committee Meeting scheduled.', time: isoDaysFromNow(-2, 17) },
  ],
};

const clone = (value) => JSON.parse(JSON.stringify(value));

export const getMockDb = () => {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (!stored) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(seed));
    return clone(seed);
  }
  try {
    return JSON.parse(stored);
  } catch {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(seed));
    return clone(seed);
  }
};

export const saveMockDb = (db) => localStorage.setItem(STORAGE_KEY, JSON.stringify(db));
export const resetMockDb = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(seed));
  return clone(seed);
};
export const wait = (ms = 250) => new Promise((resolve) => setTimeout(resolve, ms));
export const nextId = (items) => Math.max(0, ...items.map((item) => Number(item.id) || 0)) + 1;
