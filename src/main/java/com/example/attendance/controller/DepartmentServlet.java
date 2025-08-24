package com.example.attendance.controller;

import java.io.IOException;

import com.example.attendance.dao.DepartmentDAO;
import com.example.attendance.dto.Department;
import com.example.attendance.dto.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 部署管理機能を提供するサーブレット
 */
@WebServlet("/departments")
public class DepartmentServlet extends HttpServlet {
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 管理者のみアクセス可能
        HttpSession session = req.getSession(false);
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        String action = req.getParameter("action");
        if ("edit".equals(action)) {
            String departmentId = req.getParameter("departmentId");
            Department departmentToEdit = departmentDAO.findByDepartmentId(departmentId);
            req.setAttribute("departmentToEdit", departmentToEdit);
        }

        // メッセージをセッションからリクエストスコープへ移動
        String successMessage = (String) session.getAttribute("successMessage");
        if (successMessage != null) {
            req.setAttribute("successMessage", successMessage);
            session.removeAttribute("successMessage");
        }
        String errorMessage = (String) session.getAttribute("errorMessage");
        if (errorMessage != null) {
            req.setAttribute("errorMessage", errorMessage);
            session.removeAttribute("errorMessage");
        }

        req.setAttribute("departments", departmentDAO.getAllDepartments());
        req.getRequestDispatcher("/jsp/department_management.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        User currentUser = (User) session.getAttribute("user");

        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        String action = req.getParameter("action");

        switch (action) {
            case "add":
                handleAddDepartment(req, session);
                break;
            case "update":
                handleUpdateDepartment(req, session);
                break;
            case "delete":
                String departmentIdToDelete = req.getParameter("departmentId");
                try {
                    departmentDAO.deleteDepartment(departmentIdToDelete);
                    session.setAttribute("successMessage", "部署を削除しました。");
                } catch (Exception e) {
                    session.setAttribute("errorMessage", "部署の削除に失敗しました。この部署に所属するユーザーがいる可能性があります。");
                }
                break;
        }
        resp.sendRedirect(req.getContextPath() + "/departments");
    }

    private void handleAddDepartment(HttpServletRequest req, HttpSession session) {
        String departmentId = req.getParameter("departmentId");
        if (departmentDAO.findByDepartmentId(departmentId) != null) {
            session.setAttribute("errorMessage", "部署ID '" + departmentId + "' は既に使用されています。");
            return;
        }
        String departmentName = req.getParameter("departmentName");
        String description = req.getParameter("description");
        boolean enabled = "on".equals(req.getParameter("enabled"));
        
        Department newDepartment = new Department(departmentId, departmentName, description, enabled);
        departmentDAO.addDepartment(newDepartment);
        session.setAttribute("successMessage", "部署 '" + departmentName + "' を追加しました。");
    }

    private void handleUpdateDepartment(HttpServletRequest req, HttpSession session) {
        String departmentId = req.getParameter("departmentId");
        Department existingDepartment = departmentDAO.findByDepartmentId(departmentId);
        if (existingDepartment != null) {
            String departmentName = req.getParameter("departmentName");
            String description = req.getParameter("description");
            boolean enabled = "on".equals(req.getParameter("enabled"));
            
            existingDepartment.setDepartmentName(departmentName);
            existingDepartment.setDescription(description);
            existingDepartment.setEnabled(enabled);
            
            departmentDAO.updateDepartment(existingDepartment);
            session.setAttribute("successMessage", "部署 '" + departmentName + "' の情報を更新しました。");
        }
    }
}
