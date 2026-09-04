import http, { unwrap } from "./http.js";
import { API_ROUTES, USE_MOCK_API } from "../config/api.js";
import { getMockDb, nextId, saveMockDb, wait } from "./mockDb.js";

const addActivity = (db, type, title, detail) => {
  db.activities.unshift({
    id: nextId(db.activities),
    type,
    title,
    detail,
    time: new Date().toISOString(),
  });
  db.activities = db.activities.slice(0, 20);
};

const mapResident = (item) => ({
  id: item.residentId,
  name: [item.firstName, item.lastName].filter(Boolean).join(" "),
  email: item.email,
  mobile: item.mobile,
  wing: item.wing,
  flatNo: item.flatNumber,
  memberType: item.residentType,
  residentType: item.residentType,
  familyMembers: item.familyMembers ?? 0,
  status: item.moveOutDate ? "MOVED_OUT" : "ACTIVE",
  moveInDate: item.moveInDate,
  userId: item.userId,
  flatId: item.flatId,
  occupation: item.occupation,
  emergencyContact: item.emergencyContact,
  isPrimaryMember: item.isPrimaryMember,
});

const mapVisitor = (item) => ({
  id: item.visitorId,
  residentId: item.residentId,
  name: item.visitorName,
  mobile: item.mobile,
  purpose: item.purpose,
  residentName: item.residentName,
  flat: item.flatNumber,
  vehicleNo: item.vehicleNumber,
  status: item.status === "REQUESTED" ? "PENDING" : item.status,
  checkIn: item.checkInTime,
  checkOut: item.checkOutTime,
  visitDate: item.visitDate,
  expectedTime: item.expectedTime,
  passCode: item.passCode ?? `SSC-V${item.visitorId}`,
});

const mapComplaint = (item) => ({
  ...item,
  id: item.complaintId,
  raisedBy: item.residentName,
  flat: item.flatNumber ?? "",
  createdAt: item.createdAt,
});

const mapBill = (item) => ({
  id: item.billingId,
  billNo: item.billingId,
  residentId: item.residentId,
  residentName: item.residentName,
  flat: item.flatNumber,
  month:
    item.billingMonth && item.billingYear
      ? `${item.billingMonth}/${item.billingYear}`
      : "",
  billingMonth: item.billingMonth,
  billingYear: item.billingYear,
  maintenance: item.maintenanceAmount,
  water: item.waterCharge,
  electricity: item.electricityCharge,
  parking: item.parkingCharge,
  penalty: item.penalty,
  otherCharge: item.otherCharge,
  total: item.totalAmount,
  dueDate: item.dueDate,
  status: item.status,
  paidAt: item.paidAt,
  paymentMode: item.paymentMode,
  remarks: item.remarks,
  createdAt: item.createdAt,
  updatedAt: item.updatedAt,
});

const mapMeeting = (item) => ({
  ...item,
  id: item.meetingUuid,
  date:
    item.meetingDate && item.startTime
      ? `${item.meetingDate}T${item.startTime}`
      : item.meetingDate,
  time: item.startTime,
  attendees: item.attendeeCount ?? item.attendees ?? 0,
  type: item.meetingType || item.type || "GENERAL_BODY",
  status:
    item.status === "SCHEDULED"
      ? "UPCOMING"
      : item.status === "ONGOING"
        ? "IN_PROGRESS"
        : item.status,
  audience: item.audience || "ALL_RESIDENTS",
  mode: item.meetingMode || item.mode || "IN_PERSON",
  meetingLink: item.meetingLink || item.meeting_link || null,
});

const toDashboard = (role, data) => {
  const statsByRole = {
    ADMIN: {
      residents: data.activeResidents ?? data.totalResidents ?? 0,
      visitorsToday: data.todayVisitors ?? 0,
      openComplaints: data.openComplaints ?? 0,
      pendingDues: data.pendingAmount ?? 0,
      collection: data.totalCollection ?? 0,
      upcomingMeetings: 0,
    },
    RESIDENT: {
      residents: 1,
      visitorsToday: data.todayVisitors ?? data.expectedVisitors ?? 0,
      openComplaints: data.openComplaints ?? 0,
      pendingDues: data.outstandingAmount ?? 0,
      collection: 0,
      upcomingMeetings: 0,
    },
    SECURITY: {
      residents: 0,
      visitorsToday: data.todayVisitors ?? data.totalVisitorsToday ?? 0,
      openComplaints: 0,
      pendingDues: 0,
      collection: 0,
      upcomingMeetings: 0,
    },
    ACCOUNTANT: {
      residents: 0,
      visitorsToday: 0,
      openComplaints: 0,
      pendingDues: data.pendingAmount ?? data.pendingCollection ?? 0,
      collection: data.totalCollection ?? data.monthlyCollection ?? 0,
      upcomingMeetings: 0,
    },
    SECRETARY: {
      residents: data.activeResidents ?? data.totalResidents ?? 0,
      visitorsToday: data.todayVisitors ?? 0,
      openComplaints: data.openComplaints ?? 0,
      pendingDues: data.pendingAmount ?? 0,
      collection: data.totalCollection ?? 0,
      upcomingMeetings: 0,
    },
  };

  return {
    stats: statsByRole[role] ?? statsByRole.RESIDENT,
    complaintByStatus:
      role === "ADMIN"
        ? [
            { name: "OPEN", value: data.openComplaints ?? 0 },
            { name: "IN PROGRESS", value: data.inProgressComplaints ?? 0 },
            { name: "RESOLVED", value: data.resolvedComplaints ?? 0 },
          ]
        : [],
    collectionTrend: [],
    recentActivities: [],
    upcomingMeetings: [],
  };
};

const buildManagerDashboard = (summary, complaints, bills, meetings) => {
  const now = new Date();
  const months = Array.from({ length: 6 }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (5 - index), 1);
    return {
      month: date.toLocaleString("en-IN", { month: "short" }),
      monthNumber: date.getMonth() + 1,
      year: date.getFullYear(),
      collected: 0,
      pending: 0,
    };
  });

  bills.forEach((bill) => {
    const bucket = months.find(
      (item) =>
        item.monthNumber === Number(bill.billingMonth) &&
        item.year === Number(bill.billingYear),
    );
    if (!bucket) return;
    const amount = Number(bill.totalAmount || 0);
    if (bill.status === "PAID") bucket.collected += amount;
    else bucket.pending += amount;
  });

  const complaintByStatus = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]
    .map((status) => ({
      name: status.replace("_", " "),
      value: complaints.filter((complaint) => complaint.status === status)
        .length,
    }))
    .filter(
      (item) =>
        item.value > 0 ||
        ["OPEN", "IN_PROGRESS", "RESOLVED"].includes(
          item.name.replace(" ", "_"),
        ),
    );

  const recentActivities = [
    ...complaints.map((complaint) => ({
      id: `complaint-${complaint.complaintId}`,
      type: "complaint",
      title: `Complaint #${complaint.complaintId} is ${String(
        complaint.status || "OPEN",
      )
        .replace("_", " ")
        .toLowerCase()}`,
      detail:
        complaint.title || complaint.category || "Resident complaint updated",
      time: complaint.updatedAt || complaint.createdAt,
    })),
    ...bills
      .filter((bill) => bill.paidAt)
      .map((bill) => ({
        id: `payment-${bill.billingId}`,
        type: "payment",
        title: `Maintenance payment received`,
        detail: `Bill #${bill.billingId} · ₹${Number(bill.totalAmount || 0).toLocaleString("en-IN")}`,
        time: bill.paidAt,
      })),
    ...meetings.map((meeting) => ({
      id: `meeting-${meeting.meetingUuid}`,
      type: "meeting",
      title: "Meeting scheduled",
      detail: meeting.title || "Society meeting",
      time: meeting.createdAt || meeting.meetingDate,
    })),
  ]
    .filter((activity) => activity.time)
    .sort((a, b) => new Date(b.time) - new Date(a.time));

  const upcomingMeetings = meetings
    .filter(
      (meeting) =>
        meeting.status === "SCHEDULED" || meeting.status === "UPCOMING",
    )
    .map(mapMeeting)
    .slice(0, 4);

  return {
    ...toDashboard("ADMIN", summary),
    stats: {
      ...toDashboard("ADMIN", summary).stats,
      openComplaints: complaints.filter(
        (complaint) => !["RESOLVED", "CLOSED"].includes(complaint.status),
      ).length,
      upcomingMeetings: upcomingMeetings.length,
      collection: bills
        .filter((bill) => bill.status === "PAID")
        .reduce((sum, bill) => sum + Number(bill.totalAmount || 0), 0),
      pendingDues: bills
        .filter((bill) => bill.status !== "PAID")
        .reduce((sum, bill) => sum + Number(bill.totalAmount || 0), 0),
    },
    complaintByStatus,
    collectionTrend: months.map(({ month, collected, pending }) => ({
      month,
      collected,
      pending,
    })),
    recentActivities,
    upcomingMeetings,
  };
};

const buildResidentDashboard = (
  complaints,
  bills,
  visitors,
  documents,
  meetings,
) => {
  const now = new Date();
  const months = Array.from({ length: 6 }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (5 - index), 1);
    return {
      month: date.toLocaleString("en-IN", { month: "short" }),
      monthNumber: date.getMonth() + 1,
      year: date.getFullYear(),
      collected: 0,
      pending: 0,
    };
  });
  bills.forEach((bill) => {
    const bucket = months.find(
      (item) =>
        item.monthNumber === Number(bill.billingMonth) &&
        item.year === Number(bill.billingYear),
    );
    if (!bucket) return;
    if (bill.status === "PAID")
      bucket.collected += Number(bill.totalAmount || 0);
    else bucket.pending += Number(bill.totalAmount || 0);
  });
  const upcomingMeetings = meetings
    .filter(
      (meeting) =>
        meeting.status === "SCHEDULED" || meeting.status === "UPCOMING",
    )
    .map(mapMeeting)
    .slice(0, 4);
  const recentActivities = [
    ...complaints.map((item) => ({
      id: `complaint-${item.complaintId}`,
      type: "complaint",
      title: `Your complaint is ${String(item.status || "OPEN")
        .replace("_", " ")
        .toLowerCase()}`,
      detail: item.title || item.category || "Complaint update",
      time: item.updatedAt || item.createdAt,
    })),
    ...bills
      .filter((item) => item.paidAt)
      .map((item) => ({
        id: `payment-${item.billingId}`,
        type: "payment",
        title: "Your maintenance payment was received",
        detail: `Bill #${item.billingId}`,
        time: item.paidAt,
      })),
    ...visitors.map((item) => ({
      id: `visitor-${item.visitorId}`,
      type: "visitor",
      title: `Visitor request ${String(item.status || "REQUESTED").toLowerCase()}`,
      detail: item.visitorName || "Visitor request",
      time: item.checkInTime || item.visitDate,
    })),
    ...documents.map((item) => ({
      id: `document-${item.residentDocId}`,
      type: "complaint",
      title: `Document ${String(item.verificationStatus || "PENDING").toLowerCase()}`,
      detail: item.documentName || item.documentType || "Resident document",
      time: item.verifiedAt || item.createdAt,
    })),
  ]
    .filter((item) => item.time)
    .sort((a, b) => new Date(b.time) - new Date(a.time));

  return {
    stats: {
      residents: 1,
      documents: documents.length,
      visitorsToday: visitors.filter(
        (item) => item.visitDate === now.toISOString().slice(0, 10),
      ).length,
      openComplaints: complaints.filter(
        (item) => !["RESOLVED", "CLOSED"].includes(item.status),
      ).length,
      pendingDues: bills
        .filter((item) => item.status !== "PAID")
        .reduce((sum, item) => sum + Number(item.totalAmount || 0), 0),
      collection: bills
        .filter((item) => item.status === "PAID")
        .reduce((sum, item) => sum + Number(item.totalAmount || 0), 0),
      upcomingMeetings: upcomingMeetings.length,
    },
    complaintByStatus: ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]
      .map((status) => ({
        name: status.replace("_", " "),
        value: complaints.filter((item) => item.status === status).length,
      }))
      .filter(
        (item) =>
          item.value > 0 ||
          ["OPEN", "IN PROGRESS", "RESOLVED"].includes(item.name),
      ),
    collectionTrend: months.map(({ month, collected, pending }) => ({
      month,
      collected,
      pending,
    })),
    recentActivities,
    upcomingMeetings,
  };
};

const listMock = async (collection) => {
  await wait(120);
  return getMockDb()[collection];
};

const mutateMock = async (collection, id, payload, activityTitle) => {
  await wait();
  const db = getMockDb();
  const index = db[collection].findIndex(
    (item) => String(item.id) === String(id),
  );
  if (index < 0) throw new Error("Record not found.");
  db[collection][index] = { ...db[collection][index], ...payload };
  addActivity(
    db,
    collection.slice(0, -1),
    activityTitle,
    payload.title || payload.name || `Record #${id}`,
  );
  saveMockDb(db);
  return db[collection][index];
};

const getMockPayments = async () => {
  await wait(120);
  const db = getMockDb();
  if (Array.isArray(db.payments)) return db.payments;
  return db.bills
    .filter((bill) => bill.paidAt)
    .map((bill, index) => ({
      paymentId: 600 + index,
      billingId: bill.id,
      amountPaid: bill.total,
      paymentMode: bill.paymentMode || "OTHER",
      transactionReference: `DEMO-${bill.id}`,
      paymentStatus: "SUCCESS",
      paymentDate: bill.paidAt,
    }));
};

const normalizeMockBill = (bill) => {
  if (bill.billingMonth && bill.billingYear) return bill;
  const parsedPeriod = bill.month ? new Date(`1 ${bill.month}`) : null;
  const hasValidPeriod = parsedPeriod && !Number.isNaN(parsedPeriod.getTime());
  return {
    ...bill,
    billingMonth: hasValidPeriod
      ? parsedPeriod.getMonth() + 1
      : new Date().getMonth() + 1,
    billingYear: hasValidPeriod
      ? parsedPeriod.getFullYear()
      : new Date().getFullYear(),
  };
};

export const societyService = {
  async getHealth() {
    if (USE_MOCK_API) return { status: "UP" };
    return unwrap(await http.get(API_ROUTES.health));
  },

  async listResidents() {
    if (USE_MOCK_API) return listMock("residents");
    return unwrap(await http.get(API_ROUTES.residents)).map(mapResident);
  },

  async getResidentForUser(userId) {
    if (USE_MOCK_API) {
      const resident = getMockDb().residents.find(
        (item) => String(item.userId) === String(userId),
      );
      if (!resident)
        throw new Error("No resident profile is linked to this user.");
      return resident;
    }
    return mapResident(
      unwrap(await http.get(`${API_ROUTES.residents}/user/${userId}`)),
    );
  },

  async createResident(payload) {
    if (USE_MOCK_API) {
      await wait();
      const db = getMockDb();
      const item = { id: nextId(db.residents), ...payload };
      db.residents.unshift(item);
      saveMockDb(db);
      return item;
    }
    return mapResident(unwrap(await http.post(API_ROUTES.residents, payload)));
  },

  async updateResident(id, payload) {
    if (USE_MOCK_API)
      return mutateMock("residents", id, payload, "resident updated");
    return mapResident(
      unwrap(await http.put(`${API_ROUTES.residents}/${id}`, payload)),
    );
  },

  async deleteResident(id) {
    if (USE_MOCK_API) {
      const db = getMockDb();
      db.residents = db.residents.filter(
        (item) => String(item.id) !== String(id),
      );
      saveMockDb(db);
      return;
    }
    await http.delete(`${API_ROUTES.residents}/${id}`);
  },

  async getResident(id) {
    if (USE_MOCK_API)
      return getMockDb().residents.find(
        (item) => String(item.id) === String(id),
      );
    return mapResident(unwrap(await http.get(`${API_ROUTES.residents}/${id}`)));
  },

  async listResidentsByFlat(flatId) {
    if (USE_MOCK_API)
      return getMockDb().residents.filter(
        (item) => String(item.flatId) === String(flatId),
      );
    return unwrap(await http.get(`${API_ROUTES.residents}/flat/${flatId}`)).map(
      mapResident,
    );
  },

  async moveOutResident(id) {
    if (USE_MOCK_API)
      return mutateMock(
        "residents",
        id,
        { status: "MOVED_OUT" },
        "resident moved out",
      );
    return mapResident(
      unwrap(await http.patch(`${API_ROUTES.residents}/${id}/move-out`)),
    );
  },

  async listFlats(params) {
    return unwrap(await http.get(API_ROUTES.flats, { params }));
  },
  async getFlat(id) {
    return unwrap(await http.get(`${API_ROUTES.flats}/${id}`));
  },
  async createFlat(payload) {
    return unwrap(await http.post(API_ROUTES.flats, payload));
  },
  async updateFlat(id, payload) {
    return unwrap(await http.put(`${API_ROUTES.flats}/${id}`, payload));
  },
  async deleteFlat(id) {
    await http.delete(`${API_ROUTES.flats}/${id}`);
  },
  async updateFlatStatus(id, status) {
    return unwrap(
      await http.patch(`${API_ROUTES.flats}/${id}/status`, null, {
        params: { status },
      }),
    );
  },
  async listFlatsByWing(wing) {
    return unwrap(await http.get(`${API_ROUTES.flats}/wing/${wing}`));
  },
  async listFlatsByType(type) {
    return unwrap(await http.get(`${API_ROUTES.flats}/type/${type}`));
  },
  async listFlatsByStatus(status) {
    return unwrap(await http.get(`${API_ROUTES.flats}/status/${status}`));
  },

  async getFamilyMember(id) {
    return unwrap(await http.get(`${API_ROUTES.familyMembers}/${id}`));
  },
  async createFamilyMember(payload) {
    return unwrap(await http.post(API_ROUTES.familyMembers, payload));
  },
  async updateFamilyMember(id, payload) {
    return unwrap(await http.put(`${API_ROUTES.familyMembers}/${id}`, payload));
  },
  async deleteFamilyMember(id) {
    await http.delete(`${API_ROUTES.familyMembers}/${id}`);
  },
  async listFamilyMembers(residentId) {
    return unwrap(
      await http.get(`${API_ROUTES.familyMembers}/resident/${residentId}`),
    );
  },
  async countFamilyMembers(residentId) {
    return unwrap(
      await http.get(
        `${API_ROUTES.familyMembers}/resident/${residentId}/count`,
      ),
    );
  },

  async uploadResidentDocument(payload, file) {
    const formData = new FormData();
    Object.entries(payload).forEach(([key, value]) => {
      if (value != null) formData.append(key, value);
    });
    formData.append("file", file);
    return unwrap(await http.post(`${API_ROUTES.documents}/upload`, formData));
  },
  async getResidentDocument(id) {
    return unwrap(await http.get(`${API_ROUTES.documents}/${id}`));
  },
  async getResidentDocumentFile(id) {
    return http.get(`${API_ROUTES.documents}/${id}/file`, {
      responseType: "blob",
    });
  },
  async deleteResidentDocument(id) {
    await http.delete(`${API_ROUTES.documents}/${id}`);
  },
  async verifyResidentDocument(id, payload) {
    return unwrap(
      await http.patch(`${API_ROUTES.documents}/${id}/verify`, null, {
        params: payload,
      }),
    );
  },
  async rejectResidentDocument(id, payload) {
    return unwrap(
      await http.patch(`${API_ROUTES.documents}/${id}/reject`, null, {
        params: payload,
      }),
    );
  },
  async listResidentDocuments(residentId) {
    return unwrap(
      await http.get(`${API_ROUTES.documents}/resident/${residentId}`),
    );
  },
  async listAllDocuments() {
    return unwrap(await http.get(`${API_ROUTES.documents}/all`));
  },
  async listDocumentsByStatus(status) {
    return unwrap(await http.get(`${API_ROUTES.documents}/status/${status}`));
  },
  async listSocietyDocuments() {
    return (await http.get(API_ROUTES.societyDocuments)).data;
  },
  async uploadSocietyDocument(payload, file) {
    const form = new FormData();
    Object.entries(payload).forEach(([key, value]) => {
      if (value !== "" && value !== null && value !== undefined)
        form.append(key, String(value));
    });
    form.append("file", file);
    return (
      await http.post(API_ROUTES.societyDocuments, form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
    ).data;
  },
  async publishSocietyDocument(id, published) {
    return (
      await http.patch(`${API_ROUTES.societyDocuments}/${id}/publish`, null, {
        params: { published },
      })
    ).data;
  },
  async deleteSocietyDocument(id) {
    await http.delete(`${API_ROUTES.societyDocuments}/${id}`);
  },
  async getSocietyDocumentFile(id) {
    return http.get(`${API_ROUTES.societyDocuments}/${id}/file`, {
      responseType: "blob",
    });
  },
  async listStaffDocuments() {
    return (await http.get(API_ROUTES.staffDocuments)).data;
  },
  async uploadStaffDocument(payload, file) {
    const form = new FormData();
    Object.entries(payload).forEach(([key, value]) => {
      if (value !== "" && value !== null && value !== undefined)
        form.append(key, String(value));
    });
    form.append("file", file);
    return (
      await http.post(API_ROUTES.staffDocuments, form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
    ).data;
  },
  async getStaffDocumentFile(id) {
    return http.get(`${API_ROUTES.staffDocuments}/${id}/file`, {
      responseType: "blob",
    });
  },

  async listVisitors(residentId) {
    if (USE_MOCK_API) return listMock("visitors");
    const route = residentId
      ? `${API_ROUTES.visitors.list}/resident/${residentId}`
      : API_ROUTES.visitors.list;
    return unwrap(await http.get(route)).map(mapVisitor);
  },

  async getVisitor(id) {
    return mapVisitor(
      unwrap(await http.get(`${API_ROUTES.visitors.list}/${id}`)),
    );
  },
  async listVisitorsByStatus(status) {
    return unwrap(
      await http.get(`${API_ROUTES.visitors.list}/status/${status}`),
    ).map(mapVisitor);
  },
  async listVisitorsByDate(date) {
    return unwrap(
      await http.get(`${API_ROUTES.visitors.list}/visit-date/${date}`),
    ).map(mapVisitor);
  },

  async createVisitor(payload) {
    if (USE_MOCK_API) {
      await wait();
      const db = getMockDb();
      const item = {
        id: nextId(db.visitors),
        ...payload,
        status: "PENDING",
        checkIn: null,
        checkOut: null,
        passCode: `SSC-V${Date.now().toString().slice(-6)}`,
      };
      db.visitors.unshift(item);
      saveMockDb(db);
      return item;
    }
    return mapVisitor(
      unwrap(await http.post(API_ROUTES.visitors.list, payload)),
    );
  },

  async uploadVisitorPhoto(visitorId, photo) {
    const formData = new FormData();
    formData.append("photo", photo);
    return mapVisitor(
      unwrap(
        await http.post(
          `${API_ROUTES.visitors.list}/${visitorId}/photo`,
          formData,
        ),
      ),
    );
  },
  async getVisitorPhoto(visitorId) {
    return http.get(`${API_ROUTES.visitors.list}/${visitorId}/photo`, {
      responseType: "blob",
    });
  },

  async updateVisitor(id, payload) {
    if (USE_MOCK_API)
      return mutateMock("visitors", id, payload, "visitor updated");
    if (payload.status === "APPROVED" || payload.status === "REJECTED") {
      return mapVisitor(
        unwrap(
          await http.put(API_ROUTES.visitors.approve(id), {
            approved: payload.status === "APPROVED",
            remarks: payload.remarks,
          }),
        ),
      );
    }
    if (payload.status === "CHECKED_IN") {
      return mapVisitor(
        unwrap(
          await http.put(API_ROUTES.visitors.checkIn(id), {
            vehicleNumber: payload.vehicleNo,
            remarks: payload.remarks,
          }),
        ),
      );
    }
    if (payload.status === "CHECKED_OUT") {
      return mapVisitor(
        unwrap(
          await http.put(API_ROUTES.visitors.checkOut(id), {
            remarks: payload.remarks,
          }),
        ),
      );
    }
    throw new Error(
      "The backend supports visitor updates only through approval, check-in, and check-out actions.",
    );
  },

  async deleteVisitor(id) {
    if (USE_MOCK_API) {
      const db = getMockDb();
      db.visitors = db.visitors.filter(
        (item) => String(item.id) !== String(id),
      );
      saveMockDb(db);
      return;
    }
    await http.delete(`${API_ROUTES.visitors.list}/${id}`);
  },

  async listComplaints(residentId) {
    if (USE_MOCK_API) return listMock("complaints");
    const route = residentId
      ? `${API_ROUTES.complaints}/resident/${residentId}`
      : API_ROUTES.complaints;
    return unwrap(await http.get(route)).map(mapComplaint);
  },

  async getComplaint(id) {
    return mapComplaint(
      unwrap(await http.get(`${API_ROUTES.complaints}/${id}`)),
    );
  },
  async getComplaintByNumber(number) {
    return mapComplaint(
      unwrap(await http.get(`${API_ROUTES.complaints}/number/${number}`)),
    );
  },
  async listComplaintsByStatus(status) {
    return unwrap(
      await http.get(`${API_ROUTES.complaints}/status/${status}`),
    ).map(mapComplaint);
  },
  async listComplaintsByPriority(priority) {
    return unwrap(
      await http.get(`${API_ROUTES.complaints}/priority/${priority}`),
    ).map(mapComplaint);
  },
  async listComplaintsByCategory(category) {
    return unwrap(
      await http.get(`${API_ROUTES.complaints}/category/${category}`),
    ).map(mapComplaint);
  },

  async createComplaint(payload) {
    if (USE_MOCK_API) {
      await wait();
      const db = getMockDb();
      const item = {
        id: nextId(db.complaints),
        ...payload,
        status: "OPEN",
        createdAt: new Date().toISOString(),
        assignedTo: "Unassigned",
      };
      db.complaints.unshift(item);
      saveMockDb(db);
      return item;
    }
    return mapComplaint(
      unwrap(await http.post(API_ROUTES.complaints, payload)),
    );
  },

  async updateComplaint(id, payload) {
    if (USE_MOCK_API)
      return mutateMock("complaints", id, payload, "complaint updated");
    if (payload.status)
      return mapComplaint(
        unwrap(
          await http.patch(`${API_ROUTES.complaints}/${id}/status`, payload),
        ),
      );
    return mapComplaint(
      unwrap(await http.put(`${API_ROUTES.complaints}/${id}`, payload)),
    );
  },

  async uploadComplaintAttachment(id, file) {
    const data = new FormData();
    data.append("file", file);
    return mapComplaint(
      unwrap(
        await http.post(`${API_ROUTES.complaints}/${id}/attachment`, data, {
          headers: { "Content-Type": "multipart/form-data" },
        }),
      ),
    );
  },

  async getComplaintAttachment(id) {
    return http.get(`${API_ROUTES.complaints}/${id}/attachment`, {
      responseType: "blob",
    });
  },

  async deleteComplaint(id) {
    if (USE_MOCK_API) {
      const db = getMockDb();
      db.complaints = db.complaints.filter(
        (item) => String(item.id) !== String(id),
      );
      saveMockDb(db);
      return;
    }
    await http.delete(`${API_ROUTES.complaints}/${id}`);
  },

  async listBills(residentId) {
    if (USE_MOCK_API) {
      const bills = await listMock("bills");
      return bills
        .filter(
          (bill) =>
            !residentId || String(bill.residentId) === String(residentId),
        )
        .map(normalizeMockBill);
    }
    const route = residentId
      ? `${API_ROUTES.bills}/resident/${residentId}`
      : API_ROUTES.bills;
    return unwrap(await http.get(route)).map(mapBill);
  },

  async getBill(id) {
    return mapBill(unwrap(await http.get(`${API_ROUTES.bills}/${id}`)));
  },
  async listBillsByStatus(status) {
    return unwrap(await http.get(`${API_ROUTES.bills}/status/${status}`)).map(
      mapBill,
    );
  },
  async markBillOverdue(id) {
    return mapBill(
      unwrap(await http.patch(`${API_ROUTES.bills}/${id}/overdue`)),
    );
  },

  async createBill(payload) {
    if (USE_MOCK_API) {
      await wait();
      const db = getMockDb();
      const item = { id: nextId(db.bills), ...payload };
      db.bills.unshift(item);
      saveMockDb(db);
      return item;
    }
    return mapBill(unwrap(await http.post(API_ROUTES.bills, payload)));
  },

  async updateBill(id, payload) {
    if (USE_MOCK_API) return mutateMock("bills", id, payload, "bill updated");
    return mapBill(
      unwrap(await http.put(`${API_ROUTES.bills}/${id}`, payload)),
    );
  },

  async recordPayment(payload) {
    if (USE_MOCK_API) {
      const paymentDate = new Date().toISOString();
      const db = getMockDb();
      const billIndex = db.bills.findIndex(
        (item) => String(item.id) === String(payload.billingId),
      );
      if (billIndex < 0) throw new Error("Billing record not found.");
      db.payments ??= [];
      const payment = {
        paymentId: nextId(db.payments.map((item) => ({ id: item.paymentId }))),
        ...payload,
        paymentDate,
        paymentStatus: "SUCCESS",
      };
      db.payments.unshift(payment);
      db.bills[billIndex] = {
        ...db.bills[billIndex],
        status: "PAID",
        paidAt: paymentDate,
        paymentMode: payload.paymentMode,
      };
      addActivity(
        db,
        "payment",
        "payment recorded",
        payload.transactionReference || `Bill #${payload.billingId}`,
      );
      saveMockDb(db);
      return payment;
    }
    return unwrap(await http.post(API_ROUTES.payments, payload));
  },

  async updatePaymentStatus(paymentId, payload) {
    if (USE_MOCK_API) {
      const db = getMockDb();
      const payment = db.payments?.find((item) => item.paymentId === paymentId);
      if (!payment) throw new Error("Payment record not found.");
      payment.paymentStatus = payload.status;
      if (payload.remarks) payment.remarks = payload.remarks;
      saveMockDb(db);
      return payment;
    }
    return unwrap(await http.put(`${API_ROUTES.payments}/${paymentId}/status`, payload));
  },

  async listPayments() {
    if (USE_MOCK_API) return getMockPayments();
    return unwrap(await http.get(API_ROUTES.payments));
  },
  async getPayment(id) {
    return unwrap(await http.get(`${API_ROUTES.payments}/${id}`));
  },
  async listPaymentsByBill(billingId) {
    return unwrap(
      await http.get(`${API_ROUTES.payments}/billing/${billingId}`),
    );
  },
  async createRazorpayOrder(billingId) {
    if (USE_MOCK_API)
      throw new Error("Razorpay Checkout requires the Spring Boot backend.");
    return unwrap(
      await http.post(`${API_ROUTES.razorpay}/orders/${billingId}`),
    );
  },
  async verifyRazorpayPayment(payload) {
    if (USE_MOCK_API)
      throw new Error(
        "Razorpay verification requires the Spring Boot backend.",
      );
    return unwrap(await http.post(`${API_ROUTES.razorpay}/verify`, payload));
  },

  async deleteBill(id) {
    if (USE_MOCK_API) {
      const db = getMockDb();
      db.bills = db.bills.filter((item) => String(item.id) !== String(id));
      saveMockDb(db);
      return;
    }
    await http.delete(`${API_ROUTES.bills}/${id}`);
  },

  async listMeetings() {
    if (USE_MOCK_API) return listMock("meetings");
    return unwrap(await http.get(API_ROUTES.meetings)).map(mapMeeting);
  },

  async getMeeting(id) {
    return mapMeeting(unwrap(await http.get(`${API_ROUTES.meetings}/${id}`)));
  },
  async listUpcomingMeetings() {
    return unwrap(await http.get(`${API_ROUTES.meetings}/upcoming`)).map(
      mapMeeting,
    );
  },
  async listCompletedMeetings() {
    return unwrap(await http.get(`${API_ROUTES.meetings}/completed`)).map(
      mapMeeting,
    );
  },
  async listMeetingsByStatus(status) {
    const apiStatus =
      status === "UPCOMING"
        ? "SCHEDULED"
        : status === "IN_PROGRESS"
          ? "ONGOING"
          : status;
    return unwrap(
      await http.get(`${API_ROUTES.meetings}/status/${apiStatus}`),
    ).map(mapMeeting);
  },
  async searchMeetings(keyword) {
    return unwrap(
      await http.get(`${API_ROUTES.meetings}/search`, { params: { keyword } }),
    ).map(mapMeeting);
  },
  async cancelMeeting(id) {
    return mapMeeting(
      unwrap(await http.patch(`${API_ROUTES.meetings}/${id}/cancel`)),
    );
  },
  async getMeetingMinutes(id) {
    return unwrap(await http.get(`${API_ROUTES.meetings}/${id}/minutes`));
  },
  async saveMeetingMinutes(id, payload) {
    return unwrap(
      await http.post(`${API_ROUTES.meetings}/${id}/minutes`, payload),
    );
  },
  async submitMeetingAttendance(meetingId, residentId, payload) {
    return unwrap(
      await http.post(
        `${API_ROUTES.meetings}/${meetingId}/attendance/${residentId}`,
        payload,
      ),
    );
  },
  async updateMeetingAttendance(meetingId, residentId, payload) {
    return unwrap(
      await http.put(
        `${API_ROUTES.meetings}/${meetingId}/attendance/${residentId}`,
        payload,
      ),
    );
  },
  async deleteMeetingAttendance(meetingId, residentId) {
    await http.delete(
      `${API_ROUTES.meetings}/${meetingId}/attendance/${residentId}`,
    );
  },
  async listMeetingAttendance(meetingId) {
    return unwrap(
      await http.get(`${API_ROUTES.meetings}/${meetingId}/attendance`),
    );
  },
  async getMyMeetingAttendance(meetingId, residentId) {
    try {
      const response = unwrap(
        await http.get(
          `${API_ROUTES.meetings}/${meetingId}/attendance/resident/${residentId}`,
        ),
      );
      return response?.attendanceStatus ? response : null;
    } catch (error) {
      // No prior response is a normal state for a newly invited resident.
      if (error.response?.status === 404) return null;
      throw error;
    }
  },
  async getResidentAttendanceHistory(residentId) {
    return unwrap(
      await http.get(
        `${API_ROUTES.meetings}/attendance/resident/${residentId}`,
      ),
    );
  },
  async getMeetingAttendanceCount(meetingId, status) {
    return unwrap(
      await http.get(
        `${API_ROUTES.meetings}/${meetingId}/attendance/count/${status.toLowerCase().replace("_", "-")}`,
      ),
    );
  },

  async createMeeting(payload) {
    if (USE_MOCK_API) {
      await wait();
      const db = getMockDb();
      const item = {
        id: nextId(db.meetings),
        ...payload,
        status: "UPCOMING",
        attendees: 0,
      };
      db.meetings.unshift(item);
      saveMockDb(db);
      return item;
    }
    return mapMeeting(unwrap(await http.post(API_ROUTES.meetings, payload)));
  },

  async updateMeeting(id, payload) {
    if (USE_MOCK_API)
      return mutateMock("meetings", id, payload, "meeting updated");
    return mapMeeting(
      unwrap(await http.put(`${API_ROUTES.meetings}/${id}`, payload)),
    );
  },

  async updateMeetingStatus(id, status) {
    if (USE_MOCK_API)
      return mutateMock("meetings", id, { status }, "meeting status updated");
    return mapMeeting(
      unwrap(
        await http.patch(`${API_ROUTES.meetings}/${id}/status`, null, {
          params: { status },
        }),
      ),
    );
  },

  async deleteMeeting(id) {
    if (USE_MOCK_API) {
      const db = getMockDb();
      db.meetings = db.meetings.filter(
        (item) => String(item.id) !== String(id),
      );
      saveMockDb(db);
      return;
    }
    await http.delete(`${API_ROUTES.meetings}/${id}`);
  },

  async listNotifications() {
    if (USE_MOCK_API) return [];
    return unwrap(await http.get(API_ROUTES.notifications));
  },
  async listAnnouncements() {
    return unwrap(await http.get(API_ROUTES.announcements));
  },
  async createAnnouncement(payload) {
    return unwrap(await http.post(API_ROUTES.announcements, payload));
  },

  async markNotificationRead(id) {
    if (USE_MOCK_API) return;
    await http.patch(`${API_ROUTES.notifications}/${id}/read`);
  },

  async markAllNotificationsRead() {
    if (USE_MOCK_API) return;
    await http.patch(`${API_ROUTES.notifications}/read-all`);
  },

  async getDashboard(role, userId) {
    if (!USE_MOCK_API) {
      const routeByRole = {
        ADMIN: API_ROUTES.dashboard.admin,
        RESIDENT: API_ROUTES.dashboard.resident,
        SECURITY: API_ROUTES.dashboard.security,
        ACCOUNTANT: API_ROUTES.dashboard.accountant,
        // The backend exposes no separate secretary summary; committee users
        // use the society-wide admin summary.
        SECRETARY: API_ROUTES.dashboard.admin,
      };
      const route = routeByRole[role];
      if (!route)
        throw new Error(
          `No dashboard endpoint is available for the ${role || "current"} role.`,
        );
      if (["ADMIN", "SECRETARY"].includes(role)) {
        const [summary, complaints, bills, meetings] = await Promise.all([
          http.get(route).then(unwrap),
          http.get(API_ROUTES.complaints).then(unwrap),
          http.get(API_ROUTES.bills).then(unwrap),
          http.get(API_ROUTES.meetings).then(unwrap),
        ]);
        return buildManagerDashboard(summary, complaints, bills, meetings);
      }
      if (role === "RESIDENT") {
        const resident = mapResident(
          unwrap(await http.get(`${API_ROUTES.residents}/user/${userId}`)),
        );
        const [complaints, bills, visitors, documents, meetings] =
          await Promise.all([
            http
              .get(`${API_ROUTES.complaints}/resident/${resident.id}`)
              .then(unwrap),
            http
              .get(`${API_ROUTES.bills}/resident/${resident.id}`)
              .then(unwrap),
            http
              .get(`${API_ROUTES.visitors.list}/resident/${resident.id}`)
              .then(unwrap),
            http
              .get(`${API_ROUTES.documents}/resident/${resident.id}`)
              .then(unwrap),
            http.get(API_ROUTES.meetings).then(unwrap),
          ]);
        return buildResidentDashboard(
          complaints,
          bills,
          visitors,
          documents,
          meetings,
        );
      }
      return toDashboard(role, unwrap(await http.get(route)));
    }
    await wait(150);
    const db = getMockDb();
    const paid = db.bills
      .filter((bill) => bill.status === "PAID")
      .reduce((sum, bill) => sum + Number(bill.total), 0);
    const pending = db.bills
      .filter((bill) => bill.status !== "PAID")
      .reduce((sum, bill) => sum + Number(bill.total), 0);
    return {
      stats: {
        residents: db.residents.filter((item) => item.status === "ACTIVE")
          .length,
        visitorsToday: 0,
        openComplaints: db.complaints.filter(
          (item) => !["RESOLVED", "CLOSED"].includes(item.status),
        ).length,
        pendingDues: pending,
        collection: paid,
        upcomingMeetings: db.meetings.filter(
          (item) => item.status === "UPCOMING",
        ).length,
      },
      complaintByStatus: ["OPEN", "IN_PROGRESS", "RESOLVED"].map((status) => ({
        name: status.replace("_", " "),
        value: db.complaints.filter((item) => item.status === status).length,
      })),
      collectionTrend: [],
      recentActivities: db.activities,
      upcomingMeetings: db.meetings
        .filter((item) => item.status === "UPCOMING")
        .slice(0, 3),
    };
  },
};
