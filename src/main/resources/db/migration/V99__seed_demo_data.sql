-- ═══════════════════════════════════════════════════════════════
-- SkillBridge — Seed Data  (V99__seed_demo_data.sql)
-- Place at: src/main/resources/db/migration/V99__seed_demo_data.sql
-- Passwords: all demo accounts use "Demo@1234" (bcrypt encoded)
-- ═══════════════════════════════════════════════════════════════

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE notifications;
TRUNCATE TABLE community_reactions;
TRUNCATE TABLE community_messages;
TRUNCATE TABLE room_presence;
TRUNCATE TABLE revision_requests;
TRUNCATE TABLE dispute_tickets;
TRUNCATE TABLE reviews;
TRUNCATE TABLE chat_messages;
TRUNCATE TABLE portfolio_items;
TRUNCATE TABLE proposals;
TRUNCATE TABLE projects;
TRUNCATE TABLE jobs;
TRUNCATE TABLE password_reset_tokens;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ═══════════════════════════════════════════════════════════════
-- USERS
-- Password for all: Demo@1234
-- Bcrypt hash: $2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu
-- ═══════════════════════════════════════════════════════════════

INSERT INTO users (id, email, first_name, last_name, password, role,
                   active, onboarding_complete, headline, bio,
                   location, language, skills, hourly_rate,
                   experience_level, availability, portfolio_url,
                   average_rating, created_at) VALUES

-- ── Admin ──────────────────────────────────────────────────────
(1, 'admin@skillbridge.dev', 'Skill', 'Bridge', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'ADMIN', true, true,
 'Platform Administrator', 'SkillBridge platform admin account.',
 'Hyderabad, India', 'English', '[]', NULL, NULL, NULL, NULL, 0.0, NOW()),

-- ── Freelancers ────────────────────────────────────────────────
(2, 'ram.kumar@demo.com', 'Ram', 'Kumar', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'FREELANCER', true, true,
 'Full-Stack Java Developer | Spring Boot Expert',
 'I build scalable backend systems and REST APIs with 5+ years of experience in Java, Spring Boot, and MySQL. Passionate about clean architecture and SOLID principles.',
 'Hyderabad, India', 'English',
 '["Java","Spring Boot","MySQL","Docker","React","REST API","Microservices","JUnit"]',
 65.00, 'SENIOR', 'FULL_TIME',
 'https://github.com/ram-kumar', 4.9, NOW()),

(3, 'priya.reddy@demo.com', 'Priya', 'Reddy', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'FREELANCER', true, true,
 'UI/UX Designer & Frontend Developer',
 'Creative designer with 4 years building user-centred interfaces. I specialise in Figma prototyping, design systems, and translating designs into pixel-perfect HTML/CSS/React code.',
 'Bangalore, India', 'English',
 '["Figma","Adobe XD","HTML","CSS","React","Tailwind CSS","UI Design","Prototyping"]',
 55.00, 'MID', 'FULL_TIME',
 'https://dribbble.com/priyareddy', 4.7, NOW()),

(4, 'arjun.singh@demo.com', 'Arjun', 'Singh', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'FREELANCER', true, true,
 'DevOps & Cloud Engineer | AWS Certified',
 'AWS-certified DevOps engineer with expertise in CI/CD pipelines, Kubernetes, Terraform, and infrastructure-as-code. I help teams ship faster with zero-downtime deployments.',
 'Pune, India', 'English',
 '["AWS","Docker","Kubernetes","Terraform","CI/CD","Linux","Jenkins","GitHub Actions"]',
 75.00, 'SENIOR', 'PART_TIME',
 'https://github.com/arjun-devops', 4.8, NOW()),

(5, 'meera.nair@demo.com', 'Meera', 'Nair', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'FREELANCER', true, true,
 'Content Writer & SEO Specialist',
 'Experienced content strategist and writer with 6 years creating compelling copy for SaaS, e-commerce, and tech brands. Certified in Google Analytics and SEMrush.',
 'Chennai, India', 'English',
 '["Content Writing","SEO","Copywriting","Blog Writing","Email Marketing","WordPress","Research"]',
 35.00, 'SENIOR', 'FLEXIBLE',
 NULL, 4.6, NOW()),

(6, 'vikram.patel@demo.com', 'Vikram', 'Patel', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'FREELANCER', true, true,
 'Mobile Developer | Flutter & React Native',
 'Cross-platform mobile developer with published apps on both App Store and Play Store. I specialise in Flutter and React Native with Firebase backends.',
 'Mumbai, India', 'English',
 '["Flutter","React Native","Dart","Firebase","iOS","Android","REST API","Redux"]',
 60.00, 'MID', 'FULL_TIME',
 'https://github.com/vikram-mobile', 4.5, NOW()),

(7, 'ananya.das@demo.com', 'Ananya', 'Das', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'FREELANCER', true, true,
 'Data Scientist | Python & ML Engineer',
 'Data scientist with 3 years building predictive models and data pipelines. Experience with scikit-learn, TensorFlow, and deploying ML models to production with FastAPI.',
 'Delhi, India', 'English',
 '["Python","Machine Learning","TensorFlow","Pandas","SQL","FastAPI","Data Analysis","scikit-learn"]',
 70.00, 'MID', 'PART_TIME',
 'https://kaggle.com/ananya-das', 4.4, NOW()),

(8, 'kiran.joshi@demo.com', 'Kiran', 'Joshi', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'FREELANCER', true, true,
 'Graphic Designer & Brand Identity Expert',
 'Award-winning graphic designer with 7 years creating brand identities, marketing materials, and print design. Proficient in the full Adobe Creative Suite.',
 'Ahmedabad, India', 'English',
 '["Photoshop","Illustrator","InDesign","Branding","Logo Design","Print Design","Typography"]',
 45.00, 'SENIOR', 'FLEXIBLE',
 'https://behance.net/kiranjoshi', 4.8, NOW()),

-- ── Clients ────────────────────────────────────────────────────
(9, 'techcorp@demo.com', 'Rohan', 'Mehta', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'CLIENT', true, true,
 'CTO at TechCorp Solutions',
 'Building next-gen SaaS products. We hire the best freelancers for development, design, and content.',
 'Bangalore, India', 'English', '[]', NULL, NULL, NULL, NULL, 0.0, NOW()),

(10, 'startup.io@demo.com', 'Neha', 'Sharma', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'CLIENT', true, true,
 'Founder at StartupIO',
 'Early-stage startup building a fintech platform. Looking for talented freelancers to join our journey.',
 'Mumbai, India', 'English', '[]', NULL, NULL, NULL, NULL, 0.0, NOW()),

(11, 'ecommerce.brand@demo.com', 'Suresh', 'Iyer', '$2a$12$8K1p/a0dhrxSHxN1sXCOneB1.1gPIB5YGFPqX/J8F/y3V5tCbHuTu',
 'CLIENT', true, true,
 'Head of Digital at ShopEasy',
 'E-commerce brand scaling operations across India. We need reliable freelancers for ongoing digital projects.',
 'Chennai, India', 'English', '[]', NULL, NULL, NULL, NULL, 0.0, NOW());


-- ═══════════════════════════════════════════════════════════════
-- PORTFOLIO ITEMS
-- ═══════════════════════════════════════════════════════════════

INSERT INTO portfolio_items (id, owner_id, title, description, project_url, image_url, tech_stack, created_at) VALUES

-- Ram (id=2)
(1, 2, 'SkillBridge Freelance Marketplace',
 'Full-stack freelance marketplace with AI-powered proposal scoring, real-time chat via WebSocket STOMP, JWT authentication, and Cloudinary media storage.',
 'https://github.com/ram-kumar/skillbridge', NULL,
 'Spring Boot, MySQL, JWT, WebSocket, Ollama, Cloudinary', NOW()),

(2, 2, 'Inventory Management System',
 'REST API for warehouse inventory management with role-based access, PDF report generation, and automated email alerts for low stock.',
 'https://github.com/ram-kumar/inventory-api', NULL,
 'Spring Boot, MySQL, iText PDF, JavaMail', NOW()),

-- Priya (id=3)
(3, 3, 'HealthTrack Mobile App UI',
 'Complete UI/UX design for a health tracking mobile application including onboarding flow, dashboard, and activity tracking screens.',
 'https://dribbble.com/shots/healthtrack', NULL,
 'Figma, Adobe XD, Prototyping', NOW()),

(4, 3, 'SaaS Dashboard Design System',
 'Enterprise design system with 80+ reusable components, dark/light mode, and developer-ready Storybook documentation.',
 'https://dribbble.com/shots/saas-design', NULL,
 'Figma, React, Tailwind CSS, Storybook', NOW()),

-- Arjun (id=4)
(5, 4, 'Zero-Downtime Kubernetes Migration',
 'Migrated a monolithic Node.js application to Kubernetes on AWS EKS with zero downtime using blue-green deployments and Helm charts.',
 'https://github.com/arjun-devops/k8s-migration', NULL,
 'Kubernetes, AWS EKS, Helm, Docker, Terraform', NOW()),

-- Kiran (id=8)
(6, 8, 'FinTech Brand Identity Package',
 'Complete brand identity for a fintech startup: logo, colour palette, typography, business cards, and brand usage guidelines.',
 'https://behance.net/gallery/fintech-brand', NULL,
 'Illustrator, Photoshop, InDesign', NOW());


-- ═══════════════════════════════════════════════════════════════
-- JOBS
-- ═══════════════════════════════════════════════════════════════

INSERT INTO jobs (id, client_id, title, description, category, budget_min, budget_max,
                  required_skills, status, created_at) VALUES

-- TechCorp (client id=9)
(1, 9, 'Spring Boot Microservices Developer',
 'We need an experienced Spring Boot developer to help us break our monolith into microservices. You will design service boundaries, implement REST APIs with JWT auth, set up inter-service communication with Feign clients, and write unit + integration tests. Experience with Docker and MySQL required.',
 'TECHNOLOGY', 1500.00, 4000.00,
 '["Java","Spring Boot","Microservices","Docker","MySQL","JWT","REST API"]',
 'OPEN', NOW()),

(2, 9, 'AWS Infrastructure Setup & CI/CD Pipeline',
 'Set up our AWS infrastructure from scratch using Terraform. Requirements: VPC, RDS MySQL, ECS clusters, ALB, CloudFront CDN, and a GitHub Actions CI/CD pipeline with automated staging deploys and production approvals.',
 'TECHNOLOGY', 2000.00, 5000.00,
 '["AWS","Terraform","CI/CD","GitHub Actions","Docker","Linux"]',
 'OPEN', DATE_SUB(NOW(), INTERVAL 2 DAY)),

(3, 9, 'React Dashboard for Analytics Platform',
 'Build an analytics dashboard in React with Chart.js/Recharts. Must include: real-time data updates via WebSocket, responsive design, dark/light mode toggle, PDF export, and role-based feature visibility. Figma designs will be provided.',
 'TECHNOLOGY', 800.00, 2000.00,
 '["React","JavaScript","Chart.js","WebSocket","CSS","REST API"]',
 'OPEN', DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- StartupIO (client id=10)
(4, 10, 'Flutter App for Fintech Platform',
 'Build the MVP of our fintech mobile app in Flutter. Features: user registration with KYC upload, transaction history with filtering, UPI payment integration stub, push notifications via FCM, and biometric authentication.',
 'TECHNOLOGY', 2500.00, 6000.00,
 '["Flutter","Dart","Firebase","REST API","Android","iOS"]',
 'OPEN', DATE_SUB(NOW(), INTERVAL 3 DAY)),

(5, 10, 'UI/UX Design for Fintech Mobile App',
 'Design the complete UI/UX for our fintech mobile app (20+ screens). Deliverables: user research summary, low-fi wireframes, high-fi mockups in Figma, interactive prototype, and a component library.',
 'TECHNOLOGY', 600.00, 1500.00,
 '["Figma","UI Design","UX Research","Prototyping","Mobile Design"]',
 'OPEN', DATE_SUB(NOW(), INTERVAL 4 DAY)),

(6, 10, 'Content Strategy & 12-Month Blog Plan',
 'Develop a content strategy for our fintech startup blog. Deliverables: competitor analysis, keyword research (50+ target keywords), editorial calendar for 12 months (48 articles), 3 sample pillar posts, and a social media amplification plan.',
 'WRITING', 400.00, 900.00,
 '["Content Writing","SEO","Research","Content Strategy","Blog Writing"]',
 'OPEN', DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- ShopEasy (client id=11)
(7, 11, 'E-Commerce Brand Refresh & Logo Redesign',
 'Our brand needs a refresh. We want a modern, memorable logo and updated brand guidelines. Deliverables: 3 logo concepts, final logo in all formats (SVG/PNG/PDF), updated colour palette, typography guide, and brand usage document.',
 'PHOTOGRAPHY', 300.00, 800.00,
 '["Logo Design","Branding","Illustrator","Photoshop","Typography"]',
 'OPEN', DATE_SUB(NOW(), INTERVAL 6 DAY)),

(8, 11, 'Python Data Pipeline for Sales Analytics',
 'Build an ETL pipeline that pulls daily sales data from our Shopify store, transforms it, and loads it into a PostgreSQL data warehouse. Include automated anomaly detection and a simple FastAPI endpoint to serve aggregated metrics to our dashboard.',
 'TECHNOLOGY', 700.00, 1800.00,
 '["Python","ETL","PostgreSQL","FastAPI","Data Analysis","Pandas"]',
 'OPEN', DATE_SUB(NOW(), INTERVAL 7 DAY)),

(9, 11, 'Social Media Content & Product Photography',
 'Create 30 days of social media content for our fashion e-commerce brand. Includes: product photography session (in-house), 30 Instagram posts with captions + hashtags, 15 stories templates in Canva, and a monthly performance report.',
 'PHOTOGRAPHY', 250.00, 600.00,
 '["Photography","Content Creation","Social Media","Canva","Copywriting"]',
 'OPEN', DATE_SUB(NOW(), INTERVAL 8 DAY)),

-- CLOSED job (for completed project demo)
(10, 9, 'REST API for Notification Service',
 'Build a standalone notification microservice with email, SMS, and WebSocket push support.',
 'TECHNOLOGY', 500.00, 1200.00,
 '["Java","Spring Boot","WebSocket","MySQL","REST API"]',
 'CLOSED', DATE_SUB(NOW(), INTERVAL 30 DAY));


-- ═══════════════════════════════════════════════════════════════
-- PROPOSALS
-- ═══════════════════════════════════════════════════════════════

INSERT INTO proposals (id, job_id, freelancer_id, cover_letter, bid_amount, delivery_days,
                       status, ai_score, ai_badge, created_at) VALUES

-- Job 1: Spring Boot Microservices → Ram (best fit), Arjun
(1, 1, 2,
 'I have 5 years of Spring Boot experience and have led two monolith-to-microservices migrations. I recently completed a similar project decomposing a 200K-line monolith into 8 services using Domain-Driven Design. I can start immediately and will provide daily progress updates.',
 2800.00, 30, 'PENDING', 94, 'EXCELLENT_MATCH', DATE_SUB(NOW(), INTERVAL 1 DAY)),

(2, 1, 4,
 'While my primary expertise is DevOps, I have strong Java and Spring Boot skills from 3 years as a backend developer before transitioning. I can handle both the microservices development and the Docker/Kubernetes deployment setup, giving you a single contractor for the full stack.',
 3200.00, 25, 'PENDING', 78, 'GOOD_MATCH', DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- Job 2: AWS Infrastructure → Arjun (best fit)
(3, 2, 4,
 'AWS-certified DevOps engineer with 4 years of Terraform experience. I have set up identical infrastructure stacks for 3 SaaS companies — VPC, RDS, ECS, ALB, CloudFront. My GitHub Actions templates are production-tested with rollback capabilities. I can deliver in 3 weeks.',
 3500.00, 21, 'PENDING', 97, 'EXCELLENT_MATCH', DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- Job 5: UI/UX Fintech → Priya (best fit)
(4, 5, 3,
 'I have designed 3 fintech apps in the last 2 years including a payments app used by 50K users. My process: 1-day discovery call → 3 days wireframes → client approval → 7 days high-fi Figma → prototype + handoff. I deliver Figma files with developer-ready specs and assets.',
 1200.00, 14, 'PENDING', 95, 'EXCELLENT_MATCH', DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- Job 6: Content Strategy → Meera (best fit)
(5, 6, 5,
 'Fintech content is my sweet spot — I have written for 4 Indian fintech brands and understand both the regulatory nuances and the audience. I will deliver keyword research using Ahrefs + SEMrush, a full 12-month calendar in Notion, and the first 3 pillar posts within 2 weeks of kickoff.',
 750.00, 21, 'PENDING', 92, 'EXCELLENT_MATCH', DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- Job 7: Brand Redesign → Kiran (best fit)
(6, 7, 8,
 'Brand identity is my core specialty. I will present 3 distinct logo directions (wordmark, lettermark, and symbol variants) with a full rationale for each. My brand packages include SVG, PNG, EPS, PDF formats and a 20-page brand guidelines PDF. Timeline: 2 weeks.',
 650.00, 14, 'PENDING', 96, 'EXCELLENT_MATCH', DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- Job 10 (CLOSED): accepted proposal → leads to completed project
(7, 10, 2,
 'I can build the notification microservice with all three channels. I have done this before for a logistics platform that handles 10K notifications/day.',
 900.00, 20, 'ACCEPTED', 91, 'EXCELLENT_MATCH', DATE_SUB(NOW(), INTERVAL 28 DAY));


-- ═══════════════════════════════════════════════════════════════
-- PROJECTS  (from accepted proposals)
-- ═══════════════════════════════════════════════════════════════

INSERT INTO projects (id, job_id, client_id, freelancer_id, proposal_id,
                      agreed_amount, status, started_at, completed_at, created_at) VALUES

-- Completed project: TechCorp ↔ Ram (notification service)
(1, 10, 9, 2, 7,
 900.00, 'COMPLETED',
 DATE_SUB(NOW(), INTERVAL 28 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY),
 DATE_SUB(NOW(), INTERVAL 28 DAY));


-- ═══════════════════════════════════════════════════════════════
-- REVIEWS  (only on completed projects)
-- ═══════════════════════════════════════════════════════════════

INSERT INTO reviews (id, project_id, reviewer_id, reviewee_id, rating, comment, created_at) VALUES

-- Client reviews Freelancer
(1, 1, 9, 2, 5,
 'Ram delivered exceptional work. The notification service is clean, well-tested, and handles all three channels perfectly. He communicated daily, finished 3 days ahead of schedule, and even added WebSocket support that we did not originally ask for. Will hire again without hesitation.',
 DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- Freelancer reviews Client
(2, 1, 2, 9, 5,
 'Rohan was the ideal client — clear requirements, quick feedback, and always respectful of professional boundaries. Payment was released on completion day with no delays. Highly recommended for any freelancer.',
 DATE_SUB(NOW(), INTERVAL 4 DAY));


-- ═══════════════════════════════════════════════════════════════
-- UPDATE average ratings after reviews
-- ═══════════════════════════════════════════════════════════════

UPDATE users SET average_rating = 5.0 WHERE id = 2;


-- ═══════════════════════════════════════════════════════════════
-- CHAT MESSAGES  (on the completed project)
-- ═══════════════════════════════════════════════════════════════

INSERT INTO chat_messages (id, project_id, sender_id, content, is_file, created_at) VALUES

                                                                                        (1, 1, 9, 'Hi Ram, welcome to the project! I have shared the API requirements doc in the description. Let me know if you have questions.', false, DATE_SUB(NOW(), INTERVAL 27 DAY)),
                                                                                        (2, 1, 2, 'Thanks Rohan! I have reviewed the requirements. I will start with the email channel first, then WebSocket, then SMS. I will push to the repo and share daily updates here.', false, DATE_SUB(NOW(), INTERVAL 27 DAY)),
                                                                                        (3, 1, 2, 'Email channel is done and tested. 100% coverage on the service layer. Moving to WebSocket tomorrow.', false, DATE_SUB(NOW(), INTERVAL 22 DAY)),
                                                                                        (4, 1, 9, 'Tested the email flow — works perfectly. Great progress!', false, DATE_SUB(NOW(), INTERVAL 22 DAY)),
                                                                                        (5, 1, 2, 'All three channels are complete. I also added a /health endpoint and rate limiting per channel. Deploying to your staging server now.', false, DATE_SUB(NOW(), INTERVAL 8 DAY)),
                                                                                        (6, 1, 9, 'Staging looks great! Approving for production. Marking the project complete — releasing payment now. Fantastic work!', false, DATE_SUB(NOW(), INTERVAL 6 DAY));


-- ═══════════════════════════════════════════════════════════════
-- COMMUNITY MESSAGES
-- ═══════════════════════════════════════════════════════════════

INSERT INTO community_messages (id, room, sender_id, content, pinned, created_at) VALUES

-- General room
(1, 'general', 2, 'Hey everyone! Just launched SkillBridge. Excited to be part of this community 🚀', false, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2, 'general', 3, 'Welcome! The platform looks great. Love the AI-powered proposal scoring feature.', false, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(3, 'general', 1, '📌 Welcome to SkillBridge! This is the general room for introductions and off-topic chat. Check out #developers and #opportunities for work-related discussions.', true, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 'general', 5, 'Happy to be here! Content writer looking for tech clients. Anyone know good channels for finding SaaS companies?', false, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 'general', 8, 'Designers — check out #creatives for portfolio sharing and collaboration opportunities!', false, DATE_SUB(NOW(), INTERVAL 4 DAY)),

-- Developers room
(6, 'developers', 2, 'Pro tip: always use @Transactional(propagation = REQUIRES_NEW) on your notification service to avoid transaction rollback issues. Learned that the hard way 😅', false, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(7, 'developers', 4, 'Anyone using Terraform for AWS lately? The new ECS task definition syntax changed in v5 provider. Happy to share my working config.', false, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(8, 'developers', 7, 'Working on a ML pipeline with FastAPI + Pandas. The async background task approach in FastAPI is a game-changer for long-running model inference.', false, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(9, 'developers', 2, '@Arjun would love to see that Terraform config! The ECS changes broke our pipeline last week.', false, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10, 'developers', 1, '📌 Tip: Use the job board to find tech projects. The AI scoring gives you instant feedback on your proposal strength before submitting!', true, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- Opportunities room
(11, 'opportunities', 9, 'Hiring: Spring Boot developer for microservices project. ₹2.8L–4L budget. Check the job board!', false, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(12, 'opportunities', 10, 'Looking for a Flutter dev for our fintech MVP. 3-month engagement. DM me or apply on the platform.', false, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(13, 'opportunities', 3, 'Available for UI/UX work! Just finished a fintech project. DM for portfolio.', false, DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- Creatives room
(14, 'creatives', 8, 'Just published a brand identity case study on Behance. Would love feedback from fellow designers!', false, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(15, 'creatives', 3, 'The SkillBridge brief for the e-commerce brand is interesting — anyone else pitched for that logo redesign project?', false, DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- Help room
(16, 'help', 6, 'Quick question: when submitting a proposal, does the AI score update in real time or after submission?', false, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(17, 'help', 1, 'The AI scores asynchronously after you submit — you will see the score appear in the client view within a few seconds via WebSocket. The score in your own proposal list updates on refresh.', false, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(18, 'help', 6, 'Thanks! That makes sense.', false, DATE_SUB(NOW(), INTERVAL 3 DAY));


-- ═══════════════════════════════════════════════════════════════
-- NOTIFICATIONS
-- ═══════════════════════════════════════════════════════════════

INSERT INTO notifications (id, recipient_id, type, title, body, entity_id, is_read, created_at) VALUES

-- TechCorp received proposals
(1, 9, 'PROPOSAL_RECEIVED', 'New Proposal Received',
 'Ram Kumar submitted a proposal for "Spring Boot Microservices Developer"', 1, true,
 DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 9, 'PROPOSAL_RECEIVED', 'New Proposal Received',
 'Arjun Singh submitted a proposal for "Spring Boot Microservices Developer"', 2, false,
 DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 9, 'PROPOSAL_RECEIVED', 'New Proposal Received',
 'Arjun Singh submitted a proposal for "AWS Infrastructure Setup & CI/CD Pipeline"', 3, false,
 DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- Ram received acceptance on old job
(4, 2, 'PROPOSAL_ACCEPTED', 'Your Proposal Was Accepted! 🎉',
 'TechCorp Solutions accepted your proposal for "REST API for Notification Service"', 7, true,
 DATE_SUB(NOW(), INTERVAL 28 DAY)),

-- Review notifications
(5, 2, 'REVIEW_RECEIVED', 'You Received a 5-Star Review! ⭐',
 'Rohan Mehta left a 5-star review for your work on "REST API for Notification Service"', 1, true,
 DATE_SUB(NOW(), INTERVAL 4 DAY)),
(6, 9, 'REVIEW_RECEIVED', 'You Received a Review',
 'Ram Kumar left a review for your project "REST API for Notification Service"', 2, false,
 DATE_SUB(NOW(), INTERVAL 4 DAY));


-- ═══════════════════════════════════════════════════════════════
-- VERIFY
-- ═══════════════════════════════════════════════════════════════

SELECT 'Users'          AS entity, COUNT(*) AS count FROM users
UNION ALL
SELECT 'Jobs',           COUNT(*) FROM jobs
UNION ALL
SELECT 'Proposals',      COUNT(*) FROM proposals
UNION ALL
SELECT 'Projects',       COUNT(*) FROM projects
UNION ALL
SELECT 'Reviews',        COUNT(*) FROM reviews
UNION ALL
SELECT 'Portfolio',      COUNT(*) FROM portfolio_items
UNION ALL
SELECT 'Chat Messages',  COUNT(*) FROM chat_messages
UNION ALL
SELECT 'Community Msgs', COUNT(*) FROM community_messages
UNION ALL
SELECT 'Notifications',  COUNT(*) FROM notifications;
