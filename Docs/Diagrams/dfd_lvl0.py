#!/usr/bin/env python3
from pytm.pytm import TM, Actor, Dataflow, Boundary, Process

tm = TM("TeuxDeux Level 0 DFD")

b_internet = Boundary("Internet")
b_system   = Boundary("System")

user    = Actor("User",    inBoundary=b_internet)  
member  = Actor("Member",  inBoundary=b_internet)
manager = Actor("Manager", inBoundary=b_internet)
admin   = Actor("Admin",   inBoundary=b_internet)

teuxdeux = Process("TeuxDeux System", inBoundary=b_system)

Dataflow(user, teuxdeux, "Register / Login / Logout")
Dataflow(teuxdeux, user,    "JWT Token / Auth Result")

Dataflow(member, teuxdeux, "View tasks, self-assign unassigned tasks, update status, add/edit/delete comments, upload/download files")
Dataflow(teuxdeux, member,  "Projects, tasks, comments, attachments")

Dataflow(manager, teuxdeux, "Manage projects, members, tasks, comments, attachments")
Dataflow(teuxdeux, manager, "Projects, tasks, comments, attachments")

Dataflow(admin,   teuxdeux, "Manage projects, users, roles")
Dataflow(teuxdeux, admin, "Full platform data, audit logs")

tm.process()