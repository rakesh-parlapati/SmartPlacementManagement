
/* =========================================================
   SPMS - COMMON AUTHENTICATION & THEME SYSTEM
   File: /JS/auth.js
========================================================= */


/* =========================================================
   LOGIN ACCESS
========================================================= */

function requireLogin() {

    const role =
        localStorage.getItem("userRole");

    if (!role) {

        window.location.href =
            "/login.html";

        return false;
    }

    return true;
}


/* =========================================================
   STUDENT ACCESS
========================================================= */

function requireStudent() {

    const role =
        localStorage.getItem("userRole");

    const studentId =
        localStorage.getItem("studentId");


    if (
        role !== "student" ||
        !studentId
    ) {

        alert(
            "Student login is required to access this page."
        );

        window.location.href =
            "/login.html";

        return false;
    }

    return true;
}


/* =========================================================
   ADMIN ACCESS
========================================================= */

function requireAdmin() {

    const role =
        localStorage.getItem("userRole");


    if (role !== "admin") {

        alert(
            "Administrator login is required to access this page."
        );

        window.location.href =
            "/login.html";

        return false;
    }

    return true;
}


/* =========================================================
   LOGOUT
========================================================= */

async function logoutUser() {

    try {

        await fetch(
            "/auth/logout",
            {
                method: "POST",
                credentials: "same-origin"
            }
        );

    }

    catch (error) {

        console.error(
            "Logout request failed:",
            error
        );

    }

    finally {

        /* Clear login information */

        localStorage.removeItem(
            "userRole"
        );

        localStorage.removeItem(
            "studentId"
        );

        localStorage.removeItem(
            "studentName"
        );

        localStorage.removeItem(
            "studentEmail"
        );


        /* Return to login */

        window.location.href =
            "/login.html";
    }
}


/* =========================================================
   STUDENT ID
========================================================= */

function getStudentId() {

    return localStorage.getItem(
        "studentId"
    );
}


/* =========================================================
   USER ROLE
========================================================= */

function getUserRole() {

    return localStorage.getItem(
        "userRole"
    );
}


/* =========================================================
   STUDENT NAME
========================================================= */

function getStudentName() {

    return localStorage.getItem(
        "studentName"
    );
}


/* =========================================================
   STUDENT EMAIL
========================================================= */

function getStudentEmail() {

    return localStorage.getItem(
        "studentEmail"
    );
}


/* =========================================================
   GLOBAL THEME
=========================================================

   Theme values:

   light
   dark

   Storage key:

   spmsTheme

========================================================= */


/* =========================================================
   APPLY THEME
========================================================= */

function applyTheme() {

    let savedTheme =
        localStorage.getItem(
            "spmsTheme"
        );


    /*
       Backward compatibility with the
       old student dashboard theme.
    */

    if (!savedTheme) {

        const oldTheme =
            localStorage.getItem(
                "studentTheme"
            );

        if (oldTheme) {

            savedTheme =
                oldTheme;

            /*
               Move old theme to the
               new global theme system.
            */

            localStorage.setItem(
                "spmsTheme",
                savedTheme
            );

            localStorage.removeItem(
                "studentTheme"
            );
        }
    }


    /*
       Default theme = light
    */

    if (
        savedTheme !== "dark" &&
        savedTheme !== "light"
    ) {

        savedTheme = "light";

        localStorage.setItem(
            "spmsTheme",
            "light"
        );
    }


    /*
       Apply theme to body
    */

    if (
        savedTheme === "dark"
    ) {

        document.body.classList.add(
            "dark-mode"
        );

    }

    else {

        document.body.classList.remove(
            "dark-mode"
        );
    }


    /*
       Update theme button
    */

    updateThemeButton();
}


/* =========================================================
   TOGGLE DARK MODE
========================================================= */

function toggleDarkMode() {

    const isDark =
        document.body.classList.toggle(
            "dark-mode"
        );


    /*
       Save global theme
    */

    localStorage.setItem(
        "spmsTheme",
        isDark
            ? "dark"
            : "light"
    );


    /*
       Remove old theme key
    */

    localStorage.removeItem(
        "studentTheme"
    );


    /*
       Update button text
    */

    updateThemeButton();
}


/* =========================================================
   UPDATE THEME BUTTON
========================================================= */

function updateThemeButton() {

    const buttons =
        document.querySelectorAll(
            ".theme-btn"
        );


    buttons.forEach(
        function(button) {

            if (
                document.body.classList.contains(
                    "dark-mode"
                )
            ) {

                button.textContent =
                    "☀️ Light Mode";

            }

            else {

                button.textContent =
                    "🌙 Dark Mode";
            }

        }
    );
}


/* =========================================================
   AUTOMATICALLY APPLY THEME
========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    function() {

        applyTheme();

    }
);

