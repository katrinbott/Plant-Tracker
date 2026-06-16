import io
import requests
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Image, Paragraph
from reportlab.platypus.tables import Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.units import cm

BASE_URL = "http://192.168.0.138:8080"  # your app.base-url here

def main():
    try:
        plants = requests.get(f"{BASE_URL}/plants", timeout=5).json()
    except requests.exceptions.RequestException as e:  # This is the correct syntax
        print(e)
    else:
        styles = getSampleStyleSheet()
        centered = ParagraphStyle('centered', parent=styles['Normal'], alignment=TA_CENTER)
        cells = []
        for plant in plants:
            print(plant)
            qr_bytes = requests.get(f"{BASE_URL}/plants/{plant['id']}/qr").content
            img = Image(io.BytesIO(qr_bytes), width=3*cm, height=3*cm)
            name = Paragraph(f"{plant['id']}: {plant['name']}", centered)
            inner = Table([[name], [img]], colWidths=[3*cm], rowHeights=[1.5*cm, 3*cm])
            inner.setStyle(TableStyle([
                ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
                ('ALIGN', (0,0), (-1,-1), 'CENTER'),
                ('BOX', (0,1), (0,1), 0.5, colors.gray, None, (2, 2)),
            ]))
            cells.append(inner)

        # group into rows of 4 columns
        rows = [cells[i:i+4] for i in range(0, len(cells), 4)]
        # pad last row if odd number of plants
        if len(rows[-1]) == 1:
            rows[-1].append(["", ""])

        table = Table(rows, colWidths=[4.5*cm] * 4, rowHeights=[5*cm]*len(rows))
        table.setStyle(TableStyle([
            ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
            ('ALIGN', (0,0), (-1,-1), 'CENTER'),
        ]))


        filename = f"plant_qr_codes.pdf"
        doc = SimpleDocTemplate(filename, pagesize=A4)
        doc.build([table])
        print(f"Generated {filename}")


if __name__ == "__main__":
    main()
