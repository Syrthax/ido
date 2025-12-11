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
    initTodoDemo();
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
        // Remove if there's already a ripple
        const existingRipple = this.querySelector('.ripple');
        if (existingRipple) {
            existingRipple.remove();
        }
        
        const ripple = document.createElement('span');
        const rect = this.getBoundingClientRect();
        
        const size = Math.max(rect.width, rect.height);
        const x = e.clientX - rect.left - size / 2;
        const y = e.clientY - rect.top - size / 2;
        
        ripple.style.cssText = `
            position: absolute;
            width: ${size}px;
            height: ${size}px;
            left: ${x}px;
            top: ${y}px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.3);
            transform: scale(0);
            animation: rippleEffect 0.6s ease-out;
            pointer-events: none;
        `;
        
        ripple.classList.add('ripple');
        this.appendChild(ripple);
        
        setTimeout(() => {
            ripple.remove();
        }, 600);
    });
});

// Add ripple animation to styles dynamically
const style = document.createElement('style');
style.textContent = `
    @keyframes rippleEffect {
        to {
            transform: scale(2);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

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
    const dock = document.querySelector('.floating-dock');
    if (!dock) return;
    
    let lastScroll = 0;
    
    window.addEventListener('scroll', () => {
        const currentScroll = window.pageYOffset;
        
        if (currentScroll > 300) {
            dock.style.opacity = '1';
            dock.style.transform = 'translateX(-50%) translateY(0)';
        } else {
            dock.style.opacity = '0.9';
        }
        
        // Hide dock when scrolling down, show when scrolling up
        if (currentScroll > lastScroll && currentScroll > 500) {
            dock.style.transform = 'translateX(-50%) translateY(150%)';
        } else {
            dock.style.transform = 'translateX(-50%) translateY(0)';
        }
        
        lastScroll = currentScroll;
    });
}

/* ===================================================
   ANIMATED TODO DEMO
   =================================================== */

function initTodoDemo() {
    const demoTasks = document.querySelectorAll('.demo-task');
    
    if (!demoTasks.length) return;
    
    // Animate each task being checked
    demoTasks.forEach((task, index) => {
        const delay = parseInt(task.dataset.delay) || 0;
        
        setTimeout(() => {
            task.classList.add('completed');
        }, (delay + 1) * 1000);
    });
    
    // Loop the animation
    setInterval(() => {
        // Reset all tasks
        demoTasks.forEach(task => {
            task.classList.remove('completed');
        });
        
        // Re-animate
        demoTasks.forEach((task, index) => {
            const delay = parseInt(task.dataset.delay) || 0;
            
            setTimeout(() => {
                task.classList.add('completed');
            }, (delay + 1) * 1000);
        });
    }, 11000); // Repeat every 11 seconds (10s animation + 1s pause)
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
