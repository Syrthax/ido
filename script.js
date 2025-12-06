/* ===================================================
   iDo - LANDING PAGE INTERACTIONS
   =================================================== */

// Wait for DOM to load
document.addEventListener('DOMContentLoaded', () => {
    initScrollAnimations();
    initSmoothScroll();
    initParallaxEffect();
    initCursorGlow();
    initFloatingDock();
});

/* ===================================================
   SMOOTH SCROLL ANIMATIONS
   =================================================== */

function initScrollAnimations() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -100px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('active');
            }
        });
    }, observerOptions);

    // Observe all feature cards
    const featureCards = document.querySelectorAll('.feature-card');
    featureCards.forEach(card => {
        card.classList.add('scroll-reveal');
        observer.observe(card);
    });
}

/* ===================================================
   SMOOTH SCROLL TO FEATURES
   =================================================== */

function initSmoothScroll() {
    const learnMoreBtn = document.getElementById('learn-more');
    
    if (learnMoreBtn) {
        learnMoreBtn.addEventListener('click', () => {
            const featuresSection = document.getElementById('features');
            
            if (featuresSection) {
                featuresSection.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    }
}

/* ===================================================
   PARALLAX EFFECT ON SCROLL
   =================================================== */

function initParallaxEffect() {
    let ticking = false;

    window.addEventListener('scroll', () => {
        if (!ticking) {
            window.requestAnimationFrame(() => {
                const scrolled = window.pageYOffset;
                const blobs = document.querySelectorAll('.blob');
                
                blobs.forEach((blob, index) => {
                    const speed = 0.5 + (index * 0.2);
                    const yPos = -(scrolled * speed);
                    blob.style.transform = `translateY(${yPos}px)`;
                });
                
                ticking = false;
            });
            
            ticking = true;
        }
    });
}

/* ===================================================
   INTERACTIVE CURSOR GLOW EFFECT
   =================================================== */

function initCursorGlow() {
    const glassCards = document.querySelectorAll('.glass-card');
    
    glassCards.forEach(card => {
        card.addEventListener('mousemove', (e) => {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            
            card.style.setProperty('--mouse-x', `${x}px`);
            card.style.setProperty('--mouse-y', `${y}px`);
        });
    });
}

/* ===================================================
   DYNAMIC BLOB ANIMATIONS
   =================================================== */

// Add random movement to blobs
function animateBlobs() {
    const blobs = document.querySelectorAll('.blob');
    
    blobs.forEach((blob, index) => {
        const randomX = Math.random() * 100 - 50;
        const randomY = Math.random() * 100 - 50;
        const randomScale = 0.8 + Math.random() * 0.4;
        
        blob.style.animation = `float ${15 + index * 5}s infinite ease-in-out ${index * 5}s`;
    });
}

animateBlobs();

/* ===================================================
   FEATURE CARD TILT EFFECT
   =================================================== */

const featureCards = document.querySelectorAll('.feature-card');

featureCards.forEach(card => {
    card.addEventListener('mousemove', (e) => {
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        
        const centerX = rect.width / 2;
        const centerY = rect.height / 2;
        
        const rotateX = (y - centerY) / 10;
        const rotateY = (centerX - x) / 10;
        
        card.style.transform = `
            perspective(1000px)
            rotateX(${rotateX}deg)
            rotateY(${rotateY}deg)
            translateY(-5px)
            scale3d(1.02, 1.02, 1.02)
        `;
    });
    
    card.addEventListener('mouseleave', () => {
        card.style.transform = `
            perspective(1000px)
            rotateX(0deg)
            rotateY(0deg)
            translateY(0)
            scale3d(1, 1, 1)
        `;
    });
});

/* ===================================================
   BUTTON RIPPLE EFFECT
   =================================================== */

const buttons = document.querySelectorAll('.btn');

buttons.forEach(button => {
    button.addEventListener('click', function(e) {
        const ripple = document.createElement('span');
        const rect = this.getBoundingClientRect();
        
        const size = Math.max(rect.width, rect.height);
        const x = e.clientX - rect.left - size / 2;
        const y = e.clientY - rect.top - size / 2;
        
        ripple.style.width = ripple.style.height = size + 'px';
        ripple.style.left = x + 'px';
        ripple.style.top = y + 'px';
        ripple.classList.add('ripple');
        
        this.appendChild(ripple);
        
        setTimeout(() => {
            ripple.remove();
        }, 600);
    });
});

/* ===================================================
   FADE IN ON SCROLL
   =================================================== */

function fadeInOnScroll() {
    const elements = document.querySelectorAll('.scroll-reveal');
    
    elements.forEach(element => {
        const elementTop = element.getBoundingClientRect().top;
        const windowHeight = window.innerHeight;
        
        if (elementTop < windowHeight - 100) {
            element.classList.add('active');
        }
    });
}

window.addEventListener('scroll', fadeInOnScroll);
fadeInOnScroll(); // Initial check

/* ===================================================
   FLOATING DOCK - SCROLL BEHAVIOR
   =================================================== */

function initFloatingDock() {
    const dock = document.getElementById('floating-dock');
    if (!dock) return;
    
    let lastScrollTop = 0;
    let scrollThreshold = 5; // Minimum scroll distance to trigger
    let hideTimeout;
    
    function handleScroll() {
        const currentScroll = window.pageYOffset || document.documentElement.scrollTop;
        
        // Clear any existing timeout
        clearTimeout(hideTimeout);
        
        // Check if we're at the top of the page
        if (currentScroll <= 100) {
            dock.classList.remove('hidden');
            lastScrollTop = currentScroll;
            return;
        }
        
        // Determine scroll direction
        if (Math.abs(currentScroll - lastScrollTop) > scrollThreshold) {
            if (currentScroll > lastScrollTop) {
                // Scrolling down - hide dock
                dock.classList.add('hidden');
            } else {
                // Scrolling up - show dock
                dock.classList.remove('hidden');
            }
            lastScrollTop = currentScroll;
        }
    }
    
    // Use requestAnimationFrame for smoother performance
    let ticking = false;
    window.addEventListener('scroll', () => {
        if (!ticking) {
            window.requestAnimationFrame(() => {
                handleScroll();
                ticking = false;
            });
            ticking = true;
        }
    });
}

/* ===================================================
   PERFORMANCE OPTIMIZATION
   =================================================== */

// Throttle function for better performance
function throttle(func, delay) {
    let timeoutId;
    let lastExecTime = 0;
    
    return function(...args) {
        const currentTime = Date.now();
        const timeSinceLastExec = currentTime - lastExecTime;
        
        clearTimeout(timeoutId);
        
        if (timeSinceLastExec > delay) {
            lastExecTime = currentTime;
            func.apply(this, args);
        } else {
            timeoutId = setTimeout(() => {
                lastExecTime = currentTime;
                func.apply(this, args);
            }, delay - timeSinceLastExec);
        }
    };
}

// Apply throttling to scroll events
window.addEventListener('scroll', throttle(fadeInOnScroll, 100));
