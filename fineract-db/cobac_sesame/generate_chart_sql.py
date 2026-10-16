#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Générateur de Script SQL pour le Plan Comptable Fineract
======================================================
Ce script lit un fichier JSON représentant la hiérarchie du Plan Comptable EMF (PCEMF)
et génère un fichier SQL autonome qui :
  1. Désactive les vérifications de clés étrangères.
  2. Vide la table acc_gl_account.
  3. Insère les comptes dans l'ordre hiérarchique.
  4. Utilise des variables MariaDB/MySQL (ex: @parent_100 = LAST_INSERT_ID())
     pour conserver les relations parent-enfant (parent_id) dynamiquement.
"""

import json
import sys
import os

def get_classification(code):
    """
    Détermine la classification comptable Fineract (classification_enum) :
      1 = ASSET
      2 = LIABILITY
      3 = EQUITY
      4 = INCOME
      5 = EXPENSE
    """
    if not code:
        return 1
    
    first_char = code[0]
    
    if first_char == '1':
        # Classe 1 : Capitaux permanents et ressources de financement
        # - Capitaux propres, réserves, primes, subventions : Equity (3)
        # - Dettes à long terme (17, 18 sauf 181-182, 19) : Liability (2)
        if len(code) >= 2:
            prefix = code[:2]
            if prefix in ['17', '18', '19']:
                # Cas particulier : 181 et 182 sont des fonds propres assimilés (Equity)
                if code.startswith('181') or code.startswith('182'):
                    return 3
                return 2
        return 3 # Par défaut, capitaux propres pour la classe 1
        
    elif first_char == '2':
        # Classe 2 : Immobilisations
        return 1 # Asset
        
    elif first_char == '3':
        # Classe 3 : Comptes d'opérations avec la clientèle
        # - Crédits (30 à 36) : Asset (1)
        # - Dépôts/Épargne (37 à 39) : Liability (2)
        if len(code) >= 2:
            prefix = code[:2]
            if prefix in ['37', '38', '39']:
                return 2
        return 1 # Par défaut, crédits clientèle
        
    elif first_char == '4':
        # Classe 4 : Comptes de tiers
        # - Débiteurs (40 à 44) : Asset (1)
        # - Créditeurs (45 à 49) : Liability (2)
        if len(code) >= 2:
            prefix = code[:2]
            if prefix in ['45', '46', '47', '48', '49']:
                return 2
        return 1
        
    elif first_char == '5':
        # Classe 5 : Trésorerie
        # - Caisse, Banques, placements (50 à 54) : Asset (1)
        # - Découverts, dettes de trésorerie (55 à 59) : Liability (2)
        if len(code) >= 2:
            prefix = code[:2]
            if prefix in ['55', '56', '57', '58', '59']:
                return 2
        return 1
        
    elif first_char == '6':
        # Classe 6 : Comptes de charges
        return 5 # Expense
        
    elif first_char == '7':
        # Classe 7 : Comptes de produits
        return 4 # Income
        
    else:
        # Autres classes (ex: hors bilan, classe 8)
        return 1 # Par défaut Asset

def clean_name(name):
    """Échappe les apostrophes pour SQL."""
    if not name:
        return ""
    return name.replace("'", "''")

def process_accounts(json_data):
    """
    Parcourt le JSON hiérarchique et construit la liste linéaire d'inserts SQL.
    """
    sql_lines = []
    
    sql_lines.append("-- =====================================================================")
    sql_lines.append(f"-- PLAN COMPTABLE : {json_data.get('titre', 'Import automatique')}")
    sql_lines.append(f"-- Référence : {json_data.get('reference', 'N/A')}")
    sql_lines.append("-- =====================================================================")
    sql_lines.append("")
    sql_lines.append("SET FOREIGN_KEY_CHECKS = 0;")
    sql_lines.append("")
    sql_lines.append("-- Vidage de la table et des tables dépendantes (facultatif mais nécessaire pour table propre)")
    sql_lines.append("TRUNCATE TABLE acc_gl_journal_entry_annual_summary;")
    sql_lines.append("TRUNCATE TABLE m_journal_entry_aggregation_summary;")
    sql_lines.append("TRUNCATE TABLE acc_gl_financial_activity_account;")
    sql_lines.append("TRUNCATE TABLE acc_product_mapping;")
    sql_lines.append("TRUNCATE TABLE acc_accounting_rule;")
    sql_lines.append("TRUNCATE TABLE acc_gl_account;")
    sql_lines.append("")
    
    # Parcours récursif
    def insert_node(node, parent_var=None):
        code = str(node.get("numero", "")).strip()
        name = str(node.get("intitule", "")).strip()
        
        # Détecter s'il y a des enfants
        children = []
        for key in ["comptes", "sous_comptes", "details", "sous_details"]:
            if key in node:
                children.extend(node[key])
                
        # usage = 2 (Header) si enfants, 1 (Detail) si feuille
        usage = 2 if children else 1
        # Autoriser les écritures manuelles uniquement sur les détails
        manual_allowed = 1 if usage == 1 else 0
        classification = get_classification(code)
        
        parent_val = "NULL" if parent_var is None else parent_var
        
        # Ligne d'insertion
        sql_lines.append(
            f"INSERT INTO acc_gl_account (name, gl_code, disabled, manual_journal_entries_allowed, account_usage, classification_enum, parent_id, description) "
            f"VALUES ('{clean_name(name)}', '{code}', 0, {manual_allowed}, {usage}, {classification}, {parent_val}, '{clean_name(name)}');"
        )
        
        # Si cet élément a des enfants, on mémorise son ID dans une variable
        if children:
            var_name = f"@id_{code}"
            sql_lines.append(f"SET {var_name} = LAST_INSERT_ID();")
            
            for child in children:
                insert_node(child, parent_var=var_name)
                
    # Démarrer le parcours avec les classes principales
    for cls in json_data.get("classes", []):
        insert_node(cls, parent_var=None)
        sql_lines.append("") # ligne vide entre chaque classe pour la lisibilité
        
    sql_lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    sql_lines.append("")
    return "\n".join(sql_lines)

def main():
    if len(sys.argv) < 2:
        print("Usage: python generate_chart_sql.py <input_json_file> [output_sql_file]")
        sys.exit(1)
        
    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else "insert_plan_comptable.sql"
    
    if not os.path.exists(input_file):
        print(f"Error: File {input_file} not found.")
        sys.exit(1)
        
    with open(input_file, "r", encoding="utf-8") as f:
        try:
            json_data = json.load(f)
        except Exception as e:
            print(f"Error parsing JSON: {e}")
            sys.exit(1)
            
    print(f"Parsing '{json_data.get('titre')}'...")
    sql_content = process_accounts(json_data)
    
    with open(output_file, "w", encoding="utf-8") as f:
        f.write(sql_content)
        
    print(f"Successfully generated SQL script: {output_file}")

if __name__ == "__main__":
    main()
