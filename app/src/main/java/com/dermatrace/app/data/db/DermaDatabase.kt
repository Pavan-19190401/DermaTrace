package com.dermatrace.app.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.dermatrace.app.data.model.Lesion
import com.dermatrace.app.data.model.Visit
import java.util.UUID

class DermaDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "dermatrace_clinical.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_LESIONS = "lesions"
        private const val TABLE_VISITS = "visits"

        @Volatile
        private var instance: DermaDatabase? = null

        fun getInstance(context: Context): DermaDatabase {
            return instance ?: synchronized(this) {
                instance ?: DermaDatabase(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_LESIONS (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                anatomical_location TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                last_visit_date INTEGER NOT NULL,
                latest_risk_tier TEXT NOT NULL,
                latest_change_score REAL NOT NULL,
                visits_count INTEGER NOT NULL,
                thumbnail_base64 TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_VISITS (
                id TEXT PRIMARY KEY,
                lesion_id TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                image_path TEXT NOT NULL,
                change_score REAL NOT NULL,
                decision_outcome TEXT NOT NULL,
                predicted_class TEXT NOT NULL,
                confidence REAL NOT NULL,
                asymmetry REAL NOT NULL,
                border_irregularity REAL NOT NULL,
                color_variation REAL NOT NULL,
                diameter_mm REAL NOT NULL,
                evolution_score REAL NOT NULL,
                grad_cam_base64 TEXT,
                notes TEXT,
                FOREIGN KEY (lesion_id) REFERENCES $TABLE_LESIONS(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Seed initial lesions for immediate interactive review
        seedDemoData(db)
    }

    private fun seedDemoData(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L

        // Lesion 1: Left Forearm (Monitored, Stable, Low concern)
        val l1Id = "lesion-101"
        insertLesionDirect(
            db,
            Lesion(
                id = l1Id,
                name = "Left Forearm Pigment",
                anatomicalLocation = "Left Forearm",
                createdAt = now - (60 * dayMs),
                lastVisitDate = now - (4 * dayMs),
                latestRiskTier = "benign",
                latestChangeScore = 18.5f,
                visitsCount = 3
            )
        )

        // Lesion 2: Upper Back (Suspicious Evolution)
        val l2Id = "lesion-102"
        insertLesionDirect(
            db,
            Lesion(
                id = l2Id,
                name = "Upper Back Border Change",
                anatomicalLocation = "Upper Back",
                createdAt = now - (45 * dayMs),
                lastVisitDate = now - (2 * dayMs),
                latestRiskTier = "suspicious",
                latestChangeScore = 68.2f,
                visitsCount = 2
            )
        )

        // Lesion 3: Right Calf (Benign regular nevus)
        val l3Id = "lesion-103"
        insertLesionDirect(
            db,
            Lesion(
                id = l3Id,
                name = "Right Calf Nevus",
                anatomicalLocation = "Right Calf",
                createdAt = now - (90 * dayMs),
                lastVisitDate = now - (15 * dayMs),
                latestRiskTier = "benign",
                latestChangeScore = 12.0f,
                visitsCount = 2
            )
        )

        // Visits for Lesion 1
        insertVisitDirect(
            db,
            Visit(
                id = UUID.randomUUID().toString(),
                lesionId = l1Id,
                timestamp = now - (60 * dayMs),
                imagePath = "",
                changeScore = 0.0f,
                decisionOutcome = "No Visit Needed",
                predictedClass = "nv",
                confidence = 0.92f,
                asymmetry = 0.12f,
                borderIrregularity = 0.15f,
                colorVariation = 0.18f,
                diameterMm = 4.0f,
                evolutionScore = 0.0f,
                notes = "Baseline dermatoscopic capture"
            )
        )

        insertVisitDirect(
            db,
            Visit(
                id = UUID.randomUUID().toString(),
                lesionId = l1Id,
                timestamp = now - (4 * dayMs),
                imagePath = "",
                changeScore = 18.5f,
                decisionOutcome = "No Visit Needed",
                predictedClass = "nv",
                confidence = 0.89f,
                asymmetry = 0.14f,
                borderIrregularity = 0.16f,
                colorVariation = 0.19f,
                diameterMm = 4.2f,
                evolutionScore = 18.5f,
                notes = "Follow-up visit. Stable pattern, uniform pigmentation."
            )
        )
    }

    private fun insertLesionDirect(db: SQLiteDatabase, l: Lesion) {
        val cv = ContentValues().apply {
            put("id", l.id)
            put("name", l.name)
            put("anatomical_location", l.anatomicalLocation)
            put("created_at", l.createdAt)
            put("last_visit_date", l.lastVisitDate)
            put("latest_risk_tier", l.latestRiskTier)
            put("latest_change_score", l.latestChangeScore)
            put("visits_count", l.visitsCount)
            put("thumbnail_base64", l.thumbnailBase64)
        }
        db.insert(TABLE_LESIONS, null, cv)
    }

    private fun insertVisitDirect(db: SQLiteDatabase, v: Visit) {
        val cv = ContentValues().apply {
            put("id", v.id)
            put("lesion_id", v.lesionId)
            put("timestamp", v.timestamp)
            put("image_path", v.imagePath)
            put("change_score", v.changeScore)
            put("decision_outcome", v.decisionOutcome)
            put("predicted_class", v.predictedClass)
            put("confidence", v.confidence)
            put("asymmetry", v.asymmetry)
            put("border_irregularity", v.borderIrregularity)
            put("color_variation", v.colorVariation)
            put("diameter_mm", v.diameterMm)
            put("evolution_score", v.evolutionScore)
            put("grad_cam_base64", v.gradCamBase64)
            put("notes", v.notes)
        }
        db.insert(TABLE_VISITS, null, cv)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VISITS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LESIONS")
        onCreate(db)
    }

    fun getAllLesions(): List<Lesion> {
        val list = mutableListOf<Lesion>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_LESIONS ORDER BY last_visit_date DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Lesion(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        anatomicalLocation = it.getString(it.getColumnIndexOrThrow("anatomical_location")),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                        lastVisitDate = it.getLong(it.getColumnIndexOrThrow("last_visit_date")),
                        latestRiskTier = it.getString(it.getColumnIndexOrThrow("latest_risk_tier")),
                        latestChangeScore = it.getFloat(it.getColumnIndexOrThrow("latest_change_score")),
                        visitsCount = it.getInt(it.getColumnIndexOrThrow("visits_count")),
                        thumbnailBase64 = it.getString(it.getColumnIndexOrThrow("thumbnail_base64"))
                    )
                )
            }
        }
        return list
    }

    fun getVisitsForLesion(lesionId: String): List<Visit> {
        val list = mutableListOf<Visit>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_VISITS WHERE lesion_id = ? ORDER BY timestamp ASC", arrayOf(lesionId))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Visit(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        lesionId = it.getString(it.getColumnIndexOrThrow("lesion_id")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        imagePath = it.getString(it.getColumnIndexOrThrow("image_path")),
                        changeScore = it.getFloat(it.getColumnIndexOrThrow("change_score")),
                        decisionOutcome = it.getString(it.getColumnIndexOrThrow("decision_outcome")),
                        predictedClass = it.getString(it.getColumnIndexOrThrow("predicted_class")),
                        confidence = it.getFloat(it.getColumnIndexOrThrow("confidence")),
                        asymmetry = it.getFloat(it.getColumnIndexOrThrow("asymmetry")),
                        borderIrregularity = it.getFloat(it.getColumnIndexOrThrow("border_irregularity")),
                        colorVariation = it.getFloat(it.getColumnIndexOrThrow("color_variation")),
                        diameterMm = it.getFloat(it.getColumnIndexOrThrow("diameter_mm")),
                        evolutionScore = it.getFloat(it.getColumnIndexOrThrow("evolution_score")),
                        gradCamBase64 = it.getString(it.getColumnIndexOrThrow("grad_cam_base64")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes")) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun insertLesion(lesion: Lesion) {
        val db = writableDatabase
        insertLesionDirect(db, lesion)
    }

    fun insertVisit(visit: Visit) {
        val db = writableDatabase
        insertVisitDirect(db, visit)
        // Update lesion's latest visit and score
        val cv = ContentValues().apply {
            put("last_visit_date", visit.timestamp)
            put("latest_change_score", visit.changeScore)
            put("latest_risk_tier", if (visit.decisionOutcome == "Visit a Doctor") "malignant" else if (visit.decisionOutcome == "Inconclusive") "suspicious" else "benign")
        }
        db.update(TABLE_LESIONS, cv, "id = ?", arrayOf(visit.lesionId))
        db.execSQL("UPDATE $TABLE_LESIONS SET visits_count = visits_count + 1 WHERE id = ?", arrayOf(visit.lesionId))
    }
}
