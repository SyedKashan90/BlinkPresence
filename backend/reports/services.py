"""PDF (ReportLab) and Excel (OpenPyXL via pandas) attendance report
generation — FR-9. Both builders take the same flat row list so the two
formats can never drift from each other."""
from io import BytesIO

import pandas as pd
from reportlab.lib import colors
from reportlab.lib.pagesizes import landscape, letter
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

REPORT_COLUMNS = ["Registration No.", "Student Name", "Course", "Lecture Date", "Status", "Verification", "Marked At"]


def attendance_queryset_to_rows(queryset):
    rows = []
    for record in queryset.select_related("student", "lecture", "lecture__course"):
        rows.append(
            [
                record.student.registration_number,
                record.student.full_name,
                record.lecture.course.course_code,
                record.lecture.lecture_date.isoformat(),
                record.get_attendance_status_display(),
                record.get_verification_method_display(),
                record.marked_at.strftime("%Y-%m-%d %H:%M"),
            ]
        )
    return rows


def build_pdf_report(title: str, rows: list[list[str]]) -> bytes:
    buffer = BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=landscape(letter))
    styles = getSampleStyleSheet()

    elements = [Paragraph(title, styles["Title"]), Spacer(1, 12)]

    table_data = [REPORT_COLUMNS, *rows] if rows else [REPORT_COLUMNS, ["No records in range"] + [""] * (len(REPORT_COLUMNS) - 1)]
    table = Table(table_data, repeatRows=1)
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#2563EB")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("FONTSIZE", (0, 0), (-1, -1), 8),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.grey),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F8FAFC")]),
            ]
        )
    )
    elements.append(table)
    doc.build(elements)
    return buffer.getvalue()


def build_excel_report(title: str, rows: list[list[str]]) -> bytes:
    df = pd.DataFrame(rows, columns=REPORT_COLUMNS)
    buffer = BytesIO()
    with pd.ExcelWriter(buffer, engine="openpyxl") as writer:
        df.to_excel(writer, sheet_name=title[:31] or "Attendance", index=False)
        worksheet = writer.sheets[title[:31] or "Attendance"]
        for column_cells in worksheet.columns:
            length = max(len(str(cell.value)) for cell in column_cells if cell.value is not None) if any(
                cell.value is not None for cell in column_cells
            ) else 10
            worksheet.column_dimensions[column_cells[0].column_letter].width = min(max(length + 2, 12), 40)
    return buffer.getvalue()
